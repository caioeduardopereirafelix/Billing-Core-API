
set -u
cd "$(dirname "$0")/.."

[ -f .env.prod ] || { echo "missing .env.prod"; exit 2; }
set -a; . ./.env.prod; set +a

DC="docker compose --env-file .env.prod -f docker-compose.prod.yaml"
BASE="https://localhost"
CURL="curl -sk --max-time 15"

PASS=0; FAIL=0
ok()   { printf '  \033[32mPASS\033[0m  %s\n' "$1"; PASS=$((PASS+1)); }
bad()  { printf '  \033[31mFAIL\033[0m  %s\n' "$1"; FAIL=$((FAIL+1)); }
sec()  { printf '\n\033[1m%s\033[0m\n' "$1"; }

jget() { node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{try{const o=JSON.parse(s);process.stdout.write(String(o[process.argv[1]]??""))}catch(e){}})' "$1"; }
port_open() { (exec 3<>"/dev/tcp/localhost/$1") 2>/dev/null && { exec 3>&-; return 0; } || return 1; }

sec "1. Stack topology"
RUNNING=$($DC ps --status running --format '{{.Service}}' 2>/dev/null | tr '\n' ' ')
for svc in app postgres rabbitmq caddy db-backup; do
  case " $RUNNING " in *" $svc "*) ok "service '$svc' running";; *) bad "service '$svc' not running";; esac
done
case " $RUNNING " in *" pgadmin "*) bad "pgadmin running (must not be in prod)";; *) ok "pgadmin not present";; esac

sec "2. Network exposure"
port_open 5432  && bad "Postgres 5432 reachable from host"   || ok "Postgres 5432 not exposed"
port_open 5672  && bad "RabbitMQ 5672 reachable from host"    || ok "RabbitMQ 5672 not exposed"
port_open 15672 && bad "RabbitMQ mgmt 15672 reachable"        || ok "RabbitMQ mgmt 15672 not exposed"
port_open 443   && ok  "Caddy 443 reachable"                  || bad "Caddy 443 not reachable"
CERT=$(echo | openssl s_client -connect localhost:443 -servername localhost 2>/dev/null | grep -c "BEGIN CERTIFICATE")
[ "${CERT:-0}" -ge 1 ] && ok "TLS handshake on :443 serves a certificate" || bad "no certificate on :443"

sec "3. App health & migrations"
H=$($CURL "$BASE/actuator/health")
case "$H" in *'"status":"UP"'*) ok "GET /actuator/health -> UP";; *) bad "actuator health: $H";; esac
MIG=$($DC exec -T postgres psql -U "$DB_USER" -d "$DB_NAME" -tAc \
  "select count(*) from flyway_schema_history where success" 2>/dev/null | tr -d '[:space:]')
[ "$MIG" = "3" ] && ok "Flyway: 3 successful migrations" || bad "Flyway migrations = '$MIG' (expected 3)"
UID_=$($DC exec -T app id -u 2>/dev/null | tr -d '[:space:]')
[ -n "$UID_" ] && [ "$UID_" != "0" ] && ok "app runs as non-root (uid $UID_)" || bad "app uid = '$UID_'"

sec "4. Auth flow"
EMAIL="qa-$(date +%s)@example.com"
RC=$($CURL -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/register" \
  -H 'Content-Type: application/json' -d "{\"name\":\"QA User\",\"email\":\"$EMAIL\",\"password\":\"secret1\"}")
[ "$RC" = 201 ] && ok "POST /auth/register -> 201" || bad "register -> $RC"
TOK=$($CURL -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"secret1\"}" | jget token)
[ -n "$TOK" ] && ok "POST /auth/login -> token" || bad "login returned no token"
RC=$($CURL -o /dev/null -w '%{http_code}' "$BASE/plan" -H "Authorization: Bearer $TOK")
[ "$RC" = 200 ] && ok "GET /plan with token -> 200" || bad "GET /plan (token) -> $RC"
RC=$($CURL -o /dev/null -w '%{http_code}' "$BASE/plan")
[ "$RC" = 401 ] && ok "GET /plan without token -> 401" || bad "GET /plan (no token) -> $RC"
RC=$($CURL -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/login" \
  -H 'Content-Type: application/json' -d "{\"email\":\"$EMAIL\",\"password\":\"wrongpass\"}")
[ "$RC" = 401 ] && ok "login wrong password -> 401" || bad "login wrong password -> $RC"

sec "5. Admin bootstrap & RBAC"
ADM=$($CURL -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" | jget token)
[ -n "$ADM" ] && ok "bootstrapped admin can log in" || bad "admin login failed"
PLAN=$($CURL -X POST "$BASE/plan" -H "Authorization: Bearer $ADM" -H 'Content-Type: application/json' \
  -d "{\"namePlan\":\"QA-$(date +%s)\",\"description\":\"qa\",\"price\":9.9,\"billingCycle\":\"MONTHLY\"}")
PID=$(echo "$PLAN" | jget id)
[ -n "$PID" ] && ok "admin POST /plan -> 201 (id $PID)" || bad "admin create plan: $PLAN"
RC=$($CURL -o /dev/null -w '%{http_code}' -X POST "$BASE/plan" -H "Authorization: Bearer $TOK" \
  -H 'Content-Type: application/json' -d '{"namePlan":"x","description":"x","price":1,"billingCycle":"MONTHLY"}')
[ "$RC" = 403 ] && ok "USER POST /plan -> 403" || bad "USER POST /plan -> $RC"

sec "6. Subscription rules"
SUB=$($CURL -X POST "$BASE/subscription" -H "Authorization: Bearer $TOK" \
  -H 'Content-Type: application/json' -d "{\"planId\":$PID}")
case "$SUB" in *'"status":"ACTIVED"'*) ok "POST /subscription -> ACTIVED";; *) bad "subscribe: $SUB";; esac
[ "$(echo "$SUB" | jget customerEmail)" = "$EMAIL" ] && ok "subscription customerEmail = account email" || bad "customerEmail mismatch: $SUB"
$CURL -o /dev/null -X PATCH "$BASE/plan/$PID/cancel" -H "Authorization: Bearer $ADM"
RC=$($CURL -o /dev/null -w '%{http_code}' -X POST "$BASE/subscription" -H "Authorization: Bearer $TOK" \
  -H 'Content-Type: application/json' -d "{\"planId\":$PID}")
[ "$RC" = 409 ] && ok "subscribe to a disabled plan -> 409" || bad "disabled-plan subscribe -> $RC"

sec "7. Backups & frontend"
BK=$($DC exec -T db-backup sh -c 'ls -1 /backups/*.sql.gz 2>/dev/null | wc -l' | tr -d '[:space:]')
[ "${BK:-0}" -ge 1 ] && ok "db-backup produced $BK dump(s)" || bad "no backup files in /backups"
FE=$($CURL "$BASE/")
{ echo "$FE" | grep -qi bootstrap && echo "$FE" | grep -qi billing; } && ok "frontend served at /" || bad "frontend not served"

printf '\n\033[1mResult: %d passed, %d failed\033[0m\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
