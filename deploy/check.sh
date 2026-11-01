#!/usr/bin/env bash
# Checks that this server is ready to go live, and later that it really is live.
#
#   deploy/check.sh pre     # before `docker compose up`: settings, DNS, ports, mail server reachable
#   deploy/check.sh post    # after it is up: HTTPS certificate, redirect, API, security headers
#
# Prints one line per check (OK / UYARI / HATA) and exits 1 if any check is HATA.
# It only reads and connects; it changes nothing and sends no e-mail (use Ayarlar > test e-postası for that).
set -uo pipefail

cd "$(dirname "$0")"
MODE="${1:-}"
[ "$MODE" = "pre" ] || [ "$MODE" = "post" ] || { echo "Usage: $0 pre|post" >&2; exit 2; }

FAILED=0
ok() { echo "  OK     $*"; }
warn() { echo "  UYARI  $*"; }
bad() { echo "  HATA   $*"; FAILED=1; }

# Reads one value from .env without running it (see backup.sh for why).
env_value() {
  [ -f .env ] || return 0
  grep -E "^$1=" .env | tail -n 1 | cut -d= -f2- | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'\$//" || true
}

[ -f .env ] || { bad "deploy/.env yok. Önce: cp deploy/.env.example deploy/.env ve doldurun."; exit 1; }
SITE="$(env_value SITE_ADDRESS)"
URL="$(env_value PUBLIC_URL)"
HTTPS_PORT="$(env_value HTTPS_PORT)"; HTTPS_PORT="${HTTPS_PORT:-443}"
HTTP_PORT="$(env_value HTTP_PORT)"; HTTP_PORT="${HTTP_PORT:-80}"

check_settings() {
  echo "Ayarlar (deploy/.env)"
  for key in SITE_ADDRESS PUBLIC_URL DB_PASSWORD ADMIN_EMAIL MAIL_HOST MAIL_FROM; do
    [ -n "$(env_value "$key")" ] && ok "$key dolu" || bad "$key boş (zorunlu)"
  done
  case "$SITE" in
    ""|ornek.com|*example.*|localhost) bad "SITE_ADDRESS gerçek alan adınız olmalı (şu an: '${SITE:-boş}')" ;;
    http*://*) bad "SITE_ADDRESS'te https:// olmamalı, yalnızca alan adı" ;;
    *) ok "SITE_ADDRESS = $SITE" ;;
  esac
  case "$URL" in
    https://*) ok "PUBLIC_URL https ile başlıyor" ;;
    *) bad "PUBLIC_URL https:// ile başlamalı (şu an: '${URL:-boş}')" ;;
  esac
  [ "$URL" = "https://$SITE" ] || warn "PUBLIC_URL ($URL) ile SITE_ADDRESS ($SITE) aynı adres değil"
  local pw; pw="$(env_value DB_PASSWORD)"
  [ "${#pw}" -ge 16 ] && ok "DB_PASSWORD yeterince uzun" || warn "DB_PASSWORD 16 karakterden kısa"
  [ -z "$(env_value ADMIN_PASSWORD)" ] && ok "ADMIN_PASSWORD boş (ilk girişten sonra silinmiş ya da henüz ilk kurulum değil)" \
    || warn "ADMIN_PASSWORD dosyada duruyor: ilk girişten sonra şifreyi Ayarlar'dan değiştirip bu satırı silin"
  local mode; mode="$(stat -c '%a' .env 2>/dev/null || echo '')"
  [ -z "$mode" ] || [ "$mode" = "600" ] || [ "$mode" = "400" ] && ok ".env izinleri uygun" || warn ".env izinleri $mode: chmod 600 deploy/.env"
  [ -n "$(env_value PUSH_VAPID_PUBLIC)" ] && ok "Telefon bildirimleri açık (VAPID anahtarı var)" || warn "PUSH_VAPID_* boş: telefon bildirimleri kapalı (isteğe bağlı)"
  [ -n "$(env_value PAYMENT_IBAN)" ] || [ -n "$(env_value PAYMENT_IYZICO_API_KEY)" ] \
    && ok "En az bir ödeme yöntemi tanımlı" || warn "Ödeme yöntemi yok: sipariş sayfası yalnızca deneme modunda çalışır"
}

check_dns() {
  echo "Alan adı"
  local resolved; resolved="$(getent ahostsv4 "$SITE" 2>/dev/null | awk '{print $1}' | sort -u | tr '\n' ' ')"
  if [ -z "$resolved" ]; then
    bad "$SITE hiçbir adrese çözülmüyor: DNS A kaydını bu sunucunun IP'sine yönlendirin"
    return
  fi
  ok "$SITE → $resolved"
  local mine; mine="$(curl -fsS -m 8 https://api.ipify.org 2>/dev/null || true)"
  if [ -z "$mine" ]; then
    warn "Bu sunucunun dış IP'si öğrenilemedi; A kaydının doğru IP'yi gösterdiğini elle doğrulayın"
  elif echo "$resolved" | grep -qw "$mine"; then
    ok "A kaydı bu sunucuyu ($mine) gösteriyor"
  else
    bad "A kaydı ($resolved) bu sunucunun IP'si ($mine) değil: sertifika alınamaz"
  fi
}

port_in_use() { (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null; }

check_ports() {
  echo "Portlar"
  for port in "$HTTP_PORT" "$HTTPS_PORT"; do
    if port_in_use "$port"; then
      docker compose -f docker-compose.yml ps --status running -q web 2>/dev/null | grep -q . \
        && ok "Port $port dinleniyor (yığın zaten çalışıyor)" \
        || bad "Port $port başka bir program tarafından kullanılıyor"
    else
      ok "Port $port boş"
    fi
  done
}

check_mail() {
  echo "E-posta sunucusu"
  local host port; host="$(env_value MAIL_HOST)"; port="$(env_value MAIL_PORT)"; port="${port:-587}"
  [ -n "$host" ] || { bad "MAIL_HOST boş"; return; }
  if (exec 3<>"/dev/tcp/$host/$port") 2>/dev/null; then
    ok "$host:$port'a bağlanılabiliyor"
  else
    bad "$host:$port'a bağlanılamıyor (adres, port ya da sunucunun giden-port engeli?)"
    return
  fi
  if [ "$(env_value MAIL_SSL)" = "true" ] && command -v openssl >/dev/null; then
    echo | openssl s_client -connect "$host:$port" -verify_return_error >/dev/null 2>&1 \
      && ok "SMTP TLS sertifikası geçerli" || warn "SMTP TLS sertifikası doğrulanamadı"
  fi
  local from; from="$(env_value MAIL_FROM)"
  case "$from" in *@*) ok "MAIL_FROM = $from" ;; *) bad "MAIL_FROM bir e-posta adresi olmalı" ;; esac
  warn "Gerçek gönderimi bu betik denemez: yığın açıldıktan sonra Ayarlar > test e-postası ile bir e-posta gönderin"
}

check_live() {
  echo "Canlı site ($URL)"
  local code
  code="$(curl -sS -o /dev/null -m 15 -w '%{http_code}' "$URL/" 2>/dev/null || echo 000)"
  [ "$code" = "200" ] && ok "Ana sayfa 200 dönüyor" || bad "Ana sayfa $code döndü (sertifika alınamamış ya da yığın çalışmıyor olabilir)"
  code="$(curl -sS -o /dev/null -m 15 -w '%{http_code}' "$URL/api/site" 2>/dev/null || echo 000)"
  [ "$code" = "200" ] && ok "/api/site 200 dönüyor (web sunucusu backend'e ulaşıyor)" || bad "/api/site $code döndü"
  code="$(curl -sS -o /dev/null -m 15 -w '%{http_code}' "$URL/uye/yok-boyle-bir-sayfa" 2>/dev/null || echo 000)"
  [ "$code" = "200" ] && ok "Uygulama içi adresler (SPA) açılıyor" || warn "/uye/... adresi $code döndü: sayfa yenilenince 404 olabilir"

  local plain="http://${SITE}:${HTTP_PORT}/"
  [ "$HTTP_PORT" = "80" ] && plain="http://${SITE}/"
  code="$(curl -sS -o /dev/null -m 15 -w '%{http_code} %{redirect_url}' "$plain" 2>/dev/null || echo '000 ')"
  case "$code" in
    30*" https://"*) ok "http → https yönlendirmesi var" ;;
    *) warn "http adresi https'e yönlenmiyor ($code)" ;;
  esac

  if command -v openssl >/dev/null; then
    local end days
    end="$(echo | openssl s_client -servername "$SITE" -connect "$SITE:$HTTPS_PORT" 2>/dev/null | openssl x509 -noout -enddate 2>/dev/null | cut -d= -f2)"
    if [ -n "$end" ]; then
      days=$(( ( $(date -d "$end" +%s) - $(date +%s) ) / 86400 ))
      if [ "$days" -lt 0 ]; then bad "Sertifikanın süresi $((-days)) gün önce bitti"
      elif [ "$days" -lt 10 ]; then warn "Sertifika $days gün sonra bitiyor (otomatik yenilenmesi gerekir; Caddy günlüklerine bakın)"
      else ok "Sertifika geçerli, $days gün var"; fi
    else
      bad "TLS sertifikası okunamadı ($SITE:$HTTPS_PORT)"
    fi
  fi

  local headers; headers="$(curl -sSI -m 15 "$URL/" 2>/dev/null | tr -d '\r')"
  echo "$headers" | grep -qi '^strict-transport-security:' && ok "HSTS başlığı var" || warn "Strict-Transport-Security başlığı yok"
  echo "$headers" | grep -qi '^x-content-type-options: *nosniff' && ok "X-Content-Type-Options: nosniff var" || warn "X-Content-Type-Options başlığı yok"
  echo "$headers" | grep -qi '^server:.*\(tomcat\|spring\)' && warn "Sunucu başlığı yazılım adını sızdırıyor" || ok "Sunucu başlığı yazılım ayrıntısı vermiyor"
}

if [ "$MODE" = "pre" ]; then
  check_settings; check_dns; check_ports; check_mail
else
  check_live
fi

echo
if [ "$FAILED" -eq 0 ]; then echo "Sonuç: sorun yok (UYARI'lara yine de bakın)."; else echo "Sonuç: HATA var, düzeltmeden devam etmeyin."; fi
exit "$FAILED"
