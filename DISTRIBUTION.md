# Desktop distribution and Render hosting

Render hosts the download page; each user runs their own native app connected to Supabase.
The existing Docker/noVNC setup is a shared demo session, not a multi-user desktop deployment.

## Publish installers

1. Push this repository and the changes to GitHub.
2. Public project defaults are included in `src/main/resources/release-defaults.properties`.
   To override them for a release, in GitHub repository Settings → Secrets and variables → Actions → Variables,
   add `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY`. Use the public publishable key
   (or legacy anon key), never a service-role/secret key. Do not bundle an OpenAI key.
3. Push a release tag, for example `v2.1.0`. The workflow builds Windows x64,
   Apple silicon macOS and Intel macOS installers on their respective operating systems.
   Java is included. The workflow uploads installers to the GitHub release.
   Manual workflow runs produce downloadable Actions artifacts without publishing a release.
4. Verify all three installers are on the release before sharing the download page.

The repository must be public for unauthenticated GitHub release downloads.
If it remains private, host the installers somewhere with public download access
and update the three links in `website/index.html`.

## Render

In Render, create a Blueprint connected to this repository and select `render.yaml`.
It creates a static download site with `website` as the publish directory.
Alternatively choose New → Static Site, build command `echo ready`, publish directory `website`.
No backend environment variables are needed on Render: public app settings enter installers
at build time. This does not automatically replace an existing noVNC Render service.

## Local macOS package

Run `./mvnw clean package -DargLine=-Djava.awt.headless=true` with JDK 21+.
Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY if you want built-in public connection settings.
Run `python3 scripts/package-app.py --type dmg --label mac-arm64` on Apple silicon,
or use `--label mac-intel` on an Intel Mac. Find output in `target/installers`.
Each native installer must be built on its target OS/architecture.

## Signing and user data

Initial builds are unsigned. Public production distribution should use a Windows
code-signing certificate and Apple Developer signing/notarization; those credentials
are not included here. Test installation on clean Windows and macOS machines before release.
User settings and sessions live in LOCALAPPDATA/ConnectAI (Windows),
~/Library/Application Support/ConnectAI (macOS), or ~/.local/share/connectai (Linux).
The app opens Connection settings from the login sidebar when no project is bundled.
