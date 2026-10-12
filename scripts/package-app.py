#!/usr/bin/env python3
"""Package on the target OS; only public Supabase settings enter the app."""
import argparse, os, pathlib, platform, shutil, subprocess
parser = argparse.ArgumentParser()
parser.add_argument('--type', choices=['dmg', 'exe', 'app-image'], required=True)
parser.add_argument('--version', default='1.0.0')
parser.add_argument('--label', default='mac-arm64')
args = parser.parse_args()
root = pathlib.Path(__file__).resolve().parent.parent
os.chdir(root)
stage = root / 'target' / 'package-input'
shutil.rmtree(stage, ignore_errors=True)
stage.mkdir(parents=True)
shutil.copy2(root / 'target/connectai-1.0.0.jar', stage / 'connectai.jar')
url = os.environ.get('SUPABASE_URL', '').strip().rstrip('/')
key = os.environ.get('SUPABASE_PUBLISHABLE_KEY', os.environ.get('SUPABASE_ANON_KEY', '')).strip()
if bool(url) != bool(key): raise SystemExit('Provide both the Supabase URL and public key.')
if key.startswith('sb_secret_'): raise SystemExit('A secret key cannot be bundled in a desktop app.')
if key.startswith('ey'):
    import base64, json
    try:
        role = json.loads(base64.urlsafe_b64decode(key.split('.')[1] + '==='))['role']
    except Exception: raise SystemExit('Invalid legacy anon key.')
    if role != 'anon': raise SystemExit('Only a public anon key can be bundled.')
if url:
    from urllib.parse import urlparse
    parsed = urlparse(url)
    if parsed.scheme != 'https' or not parsed.hostname or parsed.path or parsed.query or parsed.fragment:
        raise SystemExit('Use the HTTPS Supabase project URL without a path.')
    if '\n' in key or '\r' in key: raise SystemExit('Invalid public key.')
    resource = root / 'target' / 'release-config'
    resource.mkdir(exist_ok=True)
    (resource / 'application.properties').write_text(f'supabase.url={url}\nsupabase.anon.key={key}\napp.mock_mode=false\n', encoding='utf-8')
    subprocess.run(['jar', 'uf', str(stage / 'connectai.jar'), '-C', str(resource), 'application.properties'], check=True)
out = root / 'target' / 'installers' / args.label
out.mkdir(parents=True, exist_ok=True)
cmd = ['jpackage', '--type', args.type, '--name', 'ConnectAI', '--app-version', args.version,
       '--input', str(stage), '--main-jar', 'connectai.jar', '--main-class', 'com.connectai.Main',
       '--dest', str(out), '--vendor', 'ConnectAI', '--description', 'Team conversations, files and decisions in one place',
       '--java-options', '-Dfile.encoding=UTF-8']
if args.type == 'exe': cmd += ['--win-per-user-install', '--win-menu', '--win-shortcut', '--win-dir-chooser']
if platform.system() == 'Darwin': cmd += ['--mac-package-identifier', 'com.connectai.desktop']
subprocess.run(cmd, check=True)
for file in out.iterdir():
    if file.suffix in ('.dmg', '.exe'):
        file.rename(out / f'ConnectAI-{args.label}{file.suffix}')
print(f'Package output: {out}')
