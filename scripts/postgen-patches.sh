#!/bin/bash
# Re-applies the skeleton's hand-maintained patches on top of freshly generated openapi
# models, plus workarounds for known openapi-generator defects. Run by generate.sh.
#
# Patches A and B were silently lost in a previous regeneration (see commits 12f1445,
# 9be7be2, 4226e97) — that is why they live here as a script rather than as manual edits.
set -euo pipefail
MODEL_DIR="$1"

# Patch A: APIHashFunction.fromValue — router sends "" / null meaning "unspecified".
python3 - "$MODEL_DIR/APIHashFunction.java" <<'PY'
import sys,re
p=sys.argv[1]; s=open(p).read()
needle="  public static APIHashFunction fromValue(String value) {\n"
guard=("    // Router historically sends \"\" or null to mean \"unspecified\". Default to UNSPECIFIED\n"
       "    // instead of throwing — see 0.27.9 fix; lost during the 0.28 OpenAPI regen.\n"
       "    if (value == null || value.isEmpty()) {\n      return UNSPECIFIED;\n    }\n")
if guard not in s:
    assert needle in s, "fromValue signature not found"
    s=s.replace(needle, needle+guard,1)
    open(p,'w').write(s)
    print("patched A: APIHashFunction")
PY

# Patch B: polymorphic deserializers must tolerate an explicit JSON null instead of throwing.
n=0
for f in "$MODEL_DIR"/*.java; do
  if grep -q 'cannot be null");' "$f"; then
    perl -0pi -e 's/throw new JsonMappingException\(ctxt\.getParser\(\), "\w+ cannot be null"\);/return null;/g' "$f"
    n=$((n+1))
  fi
done
echo "patched B: $n files"

# Patch C: openapi-generator emits `List<T>.class` / `Map<K,V>.class` for oneOf variants
# that are collections — not legal Java. Older generators emitted the raw type, which is
# what the erasure-based equals() checks here actually want.
c=0
for f in "$MODEL_DIR"/*.java; do
  if grep -qE '(List|Map)<[^<>]*>(\.class|\()|instanceof\s+(List|Map)<' "$f"; then
    perl -pi -e 's/\b(List|Map)<[^<>]*>\.class/$1.class/g' "$f"
    # ...and the matching accessor names: getList<T>() -> getListValue()
    perl -pi -e 's/\bget(List|Map)<[^<>]*>\(/get$1Value(/g' "$f"
    # ...and `instanceof List<T>` (illegal before Java 16; project targets 11)
    perl -pi -e 's/\binstanceof\s+(List|Map)<[^<>]*>/instanceof $1/g' "$f"
    c=$((c+1))
  fi
done
echo "patched C: $c files"

# Patch D: in oneOf toUrlQueryString(), the generator casts getActualInstance() to the
# *element* type before .get(key)/.get(i); it must be the *container* type. Recover the
# correct cast from the preceding null-check line, which the generator gets right.
python3 - "$MODEL_DIR" <<'PY'
import sys,os,re
md=sys.argv[1]; n=0
guard=re.compile(r'\(\((?P<t>(?:List|Map)<[^()]*?>)\)getActualInstance\(\)\)\.get\((?P<k>_key|i)\) != null')
bad=re.compile(r'joiner\.add\(\(\([^()]*?\)getActualInstance\(\)\)\.get\((?P<k>_key|i)\)')
for fn in sorted(os.listdir(md)):
    if not fn.endswith('.java'): continue
    p=os.path.join(md,fn); lines=open(p).read().split('\n'); ct=None; ch=False
    for idx,l in enumerate(lines):
        g=guard.search(l)
        if g: ct=g.group('t'); continue
        m=bad.search(l)
        if m and ct:
            lines[idx]=bad.sub(lambda mm: 'joiner.add(((%s)getActualInstance()).get(%s)'%(ct,mm.group('k')), l, count=1)
            ch=True
    if ch: open(p,'w').write('\n'.join(lines)); n+=1
print("patched D: %d files"%n)
PY

# Patch E: an empty-object oneOf variant (e.g. noneAccount: {type: object,
# additionalProperties: false}) generates as bare `Object`, which matches EVERY JSON object.
# The generator also does not enforce `required`, so the concrete variants in turn match `{}`.
# Between them the match count is never 1 and the class throws for all input. Split the two
# cases apart: the Object branch takes only an empty object, the concrete branches only a
# non-empty one.
python3 - "$MODEL_DIR" <<'PY'
import sys,os,re
md=sys.argv[1]; n=0
obj_old="""                if (attemptParsing) {
                    deserialized = tree.traverse(jp.getCodec()).readValueAs(Object.class);"""
obj_new="""                // Patch E: bare `Object` stands for an empty-object schema; without this
                // guard it matches every object and the oneOf can never resolve.
                if (attemptParsing && tree.isObject() && tree.size() == 0) {
                    deserialized = tree.traverse(jp.getCodec()).readValueAs(Object.class);"""
concrete=re.compile(
    r'                if \(attemptParsing\) \{\n'
    r'                    deserialized = tree\.traverse\(jp\.getCodec\(\)\)\.readValueAs\((?P<c>(?!Object\b)\w+)\.class\);')
def fix(m):
    return ('                // Patch E: an empty object belongs to the empty-object variant alone.\n'
            '                if (attemptParsing && !(tree.isObject() && tree.size() == 0)) {\n'
            '                    deserialized = tree.traverse(jp.getCodec()).readValueAs(%s.class);' % m.group('c'))
for fn in sorted(os.listdir(md)):
    if not fn.endswith('.java'): continue
    fp=os.path.join(md,fn); s=open(fp).read()
    if 'schemas.put("Object", Object.class)' not in s or obj_old not in s: continue
    s=s.replace(obj_old,obj_new,1)
    s=concrete.sub(fix,s)
    open(fp,'w').write(s); n+=1
print("patched E: %d files"%n)
PY

# Patch F: this adapter emitted walletAccount.type as "wallet" before the router spec
# closed the property to the single value "walletAccount". Accept the legacy spelling on
# input so previously-emitted payloads still deserialize; output always uses the new value.
python3 - "$MODEL_DIR/APIWalletLedgerAccount.java" <<'PY'
import sys
p=sys.argv[1]; s=open(p).read()
needle="""    public static TypeEnum fromValue(String value) {
      for (TypeEnum b : TypeEnum.values()) {"""
guard="""    public static TypeEnum fromValue(String value) {
      // Legacy: this adapter emitted "wallet" before the spec closed the enum.
      if ("wallet".equals(value)) {
        return WALLETACCOUNT;
      }
      for (TypeEnum b : TypeEnum.values()) {"""
if 'Legacy: this adapter emitted' not in s:
    assert needle in s, "TypeEnum.fromValue not found"
    open(p,'w').write(s.replace(needle,guard,1))
    print("patched F: APIWalletLedgerAccount")
PY
