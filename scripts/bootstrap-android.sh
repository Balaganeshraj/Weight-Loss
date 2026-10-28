#!/usr/bin/env bash
# Linux cloud build setup. Downloads only verified official distributions.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="${WELLBEING_TOOLS_DIR:-/workspace/toolchains}"
mkdir -p "$TOOLS"
export WELLBEING_TOOLS_DIR="$TOOLS"
python3 - <<'PY'
import hashlib, os, pathlib, shutil, subprocess, tarfile, zipfile, shlex, urllib.parse
p = pathlib.Path(os.environ['WELLBEING_TOOLS_DIR'])
def download(url, name, digest, algorithm='sha256'):
    path=p/name
    if not path.exists() or hashlib.new(algorithm,path.read_bytes()).hexdigest()!=digest:
        subprocess.run(['curl','--fail','--location','--retry','2',url,'--output',str(path)],check=True)
    if hashlib.new(algorithm,path.read_bytes()).hexdigest()!=digest: raise RuntimeError('Distribution checksum mismatch: '+name)
    return path
jdk=p/'jdk-17.0.16+8'
if not (jdk/'bin/javac').exists():
    archive=download('https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.16%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.16_8.tar.gz','jdk.tar.gz','166774efcf0f722f2ee18eba0039de2d685b350ee14d7b69e6f83437dafd2af1')
    with tarfile.open(archive) as t: t.extractall(p,filter='data')
sdk=p/'android-sdk'
if not (sdk/'cmdline-tools/latest/bin/sdkmanager').exists():
    archive=download('https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip','cmdline.zip','5fdcc763663eefb86a5b8879697aa6088b041e70','sha1')
    temp=p/'sdk-unpack'
    with zipfile.ZipFile(archive) as z: z.extractall(temp)
    (sdk/'cmdline-tools').mkdir(parents=True,exist_ok=True)
    shutil.move(str(temp/'cmdline-tools'),str(sdk/'cmdline-tools/latest'))
    for binary in (sdk/'cmdline-tools/latest/bin').iterdir(): binary.chmod(0o755)
# Preserve standard CA roots and add the platform's supplied CA, if present.
shutil.copy(jdk/'lib/security/cacerts',p/'cacerts')
certificate=os.environ.get('CODEX_PROXY_CERT')
if certificate:
    if not os.path.isfile(certificate):
        (p/'proxy-ca.pem').write_text(certificate); certificate=str(p/'proxy-ca.pem')
    subprocess.run([str(jdk/'bin/keytool'),'-importcert','-noprompt','-alias','cloud-egress','-file',certificate,'-keystore',str(p/'cacerts'),'-storepass','changeit'],check=True,capture_output=True)
proxy=urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY',os.environ.get('https_proxy','')))
options=f'-Djavax.net.ssl.trustStore={p}/cacerts'
if proxy.hostname:
    options+=f' -Dhttps.proxyHost={proxy.hostname} -Dhttps.proxyPort={proxy.port or 80} -Dhttp.proxyHost={proxy.hostname} -Dhttp.proxyPort={proxy.port or 80} -Dhttp.nonProxyHosts=localhost|127.*'
variables={'JAVA_HOME':str(jdk),'ANDROID_HOME':str(sdk),'GRADLE_USER_HOME':str(p/'gradle-home'),'ANDROID_USER_HOME':str(p/'android-user'),'ANDROID_EMULATOR_HOME':str(p/'android-user'),'ANDROID_AVD_HOME':str(p/'avd'),'XDG_RUNTIME_DIR':str(p/'xdg-runtime'),'JAVA_TOOL_OPTIONS':options}
for name in ['ANDROID_USER_HOME','ANDROID_AVD_HOME','XDG_RUNTIME_DIR']: pathlib.Path(variables[name]).mkdir(parents=True,exist_ok=True)
pathlib.Path(variables['XDG_RUNTIME_DIR']).chmod(0o700)
(p/'android-env.sh').write_text(''.join('export '+k+'='+shlex.quote(v)+'\n' for k,v in variables.items())+'export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"\n')
(p/'sdk-proxy.args').write_text('\n'.join(['--proxy=http','--proxy_host='+proxy.hostname,'--proxy_port='+str(proxy.port or 80)]) if proxy.hostname else '')
PY
source "$TOOLS/android-env.sh"
mapfile -t SDK_PROXY < "$TOOLS/sdk-proxy.args"
set +o pipefail
yes 2>/dev/null | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" "${SDK_PROXY[@]}" --licenses > "$TOOLS/licenses.log" 2>&1
license_status=${PIPESTATUS[1]}
set -o pipefail
if [ "$license_status" -ne 0 ]; then cat "$TOOLS/licenses.log"; exit "$license_status"; fi
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" "${SDK_PROXY[@]}" 'platforms;android-36' 'build-tools;35.0.0' 'platform-tools'
cd "$ROOT"
# Only initialize the machine-local path if the user has not supplied one.
if [ ! -f local.properties ]; then printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties; fi
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
