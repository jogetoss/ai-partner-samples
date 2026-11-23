#!/usr/bin/env bash
# Smoke test for Composer MCP over HTTP JSON-RPC. Needs only bash and curl (no LLM, no Claude Code).
#   ./smoke-test.sh [BASE_URL]            default: the JW_BASE environment variable, or http://localhost:8080/jw
#   JOGET_BUILDER_API_KEY=... ./smoke-test.sh     also runs the keyed step (builder_status)
# Exit code 0 = every step passed. Skills steps need no key; builder_* tools do.
BASE="${1:-${JW_BASE:-http://localhost:8080/jw}}"
URL="$BASE/mcp/composer/mcp"
HDR=(-H "Content-Type: application/json" -H "Accept: application/json,text/event-stream")
fail=0
ok()   { echo "PASS  $1"; }
bad()  { echo "FAIL  $1"; fail=1; }
skip() { echo "SKIP  $1"; }
custom_missing=

# 1. initialize (returns Mcp-Session-Id header)
SID=$(curl -s -i "${HDR[@]}" -X POST "$URL" -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"smoke","version":"1"}}}' \
      | grep -i '^Mcp-Session-Id' | cut -d' ' -f2 | tr -d '\r')
[ -n "$SID" ] && ok "initialize (session $SID)" || { bad "initialize: no Mcp-Session-Id (plugin not active, or wrong URL $URL)"; exit 1; }
call() { curl -s "${HDR[@]}" -H "Mcp-Session-Id: $SID" -X POST "$URL" -d "$1"; }
call '{"jsonrpc":"2.0","method":"notifications/initialized"}' >/dev/null

# 2. tools/list
T=$(call '{"jsonrpc":"2.0","id":2,"method":"tools/list"}')
for n in builder_status builder_form joget_skills_list joget_skill_read; do
  echo "$T" | grep -q "\"$n\"" && ok "tools/list has $n" || bad "tools/list misses $n"
done

# 3. skills list: bundled and Acme central skills
S=$(call '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"joget_skills_list","arguments":{}}}')
echo "$S" | grep -q 'joget-form-builder' && ok "bundled skill joget-form-builder listed" || bad "bundled skills not listed"
for n in acme-naming-conventions acme-app-standards acme-standards-lookup acme-standards-reference; do
  echo "$S" | grep -q "$n" && ok "custom skill $n listed" || { skip "custom skill $n is not installed (see ../04-custom-skills)"; custom_missing=1; }
done

# 4. read one section of a custom skill
R=$(call '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"joget_skill_read","arguments":{"name":"acme-naming-conventions","section":"FieldIds"}}}')
if [ -z "$custom_missing" ]; then
  echo "$R" | grep -q 'acme_' && ok "joget_skill_read acme-naming-conventions#FieldIds" || bad "joget_skill_read returned no acme_ rule"
else
  skip "joget_skill_read of a custom skill (custom skills are not installed)"
fi

# 5. prompts
P=$(call '{"jsonrpc":"2.0","id":5,"method":"prompts/list"}')
echo "$P" | grep -q '"name":"joget_' && ok "prompts/list returns the bundled prompts" || bad "prompts/list returned no bundled prompt"
if [ -z "$custom_missing" ]; then
  echo "$P" | grep -q 'skill_acme_naming_conventions' && ok "prompt skill_acme_naming_conventions" || bad "prompt for the custom skill is missing"
fi

# 6. keyed step
B=$(call '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"builder_status","arguments":{}}}')
if [ -n "$JOGET_BUILDER_API_KEY" ]; then
  B=$(curl -s "${HDR[@]}" -H "Mcp-Session-Id: $SID" -H "X-Builder-Api-Key: $JOGET_BUILDER_API_KEY" -X POST "$URL" -d '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"builder_status","arguments":{}}}')
  echo "$B" | grep -q '"isError":true' && bad "builder_status with key: $B" || ok "builder_status with key"
else
  echo "$B" | grep -q -E 'deny-by-default|Unauthorized' && ok "builder_status refused without a key (expected)" || bad "builder_status unexpected: $B"
  echo "SKIP  keyed builder_* steps (set JOGET_BUILDER_API_KEY, and the same key on the server)"
fi
exit $fail
