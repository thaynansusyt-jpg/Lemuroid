# KL Play

**Código do KL Play:** [branch dev](https://github.com/thaynansusyt-jpg/Lemuroid/tree/dev). Esta branch `master` preserva a base original do fork; use `dev` ou a tag da release para compilar o KL Play.

Emulador para Android e fork independente do [Lemuroid](https://github.com/Swordfish90/Lemuroid), criado por Poké Laranja / Senhor Laranja.

[Site e download](https://kl-gba-play.emilysousa65477.chatgpt.site) · [Releases](https://github.com/thaynansusyt-jpg/Lemuroid/releases) · [Relatar um problema](https://github.com/thaynansusyt-jpg/Lemuroid/issues)

## Versões

- **0.5.0-rc.1**: primeira candidata pública, com biblioteca/capas, skins, temas, perfil, Sii e diário local. Google e GitHub ainda não estão configurados.
- **0.5.0 Plus (0.5.0-plus.1)**: candidata de manutenção. Corrige limites do áudio durante avanço rápido e o estado de carregamento, reduz uploads de imagem repetidos e adiciona conta KL, backup do perfil e conjunto Sii+.

As versões são de teste. Compatibilidade e desempenho dependem do aparelho, do jogo e do núcleo; não há promessa de 8x ou 60 FPS universais.

## Instalação e atualização

Baixe **somente o APK** da versão desejada. Instale a Plus por cima do KL Play atual para manter dados. O pacote permanece `com.klgames.klgba`, com a mesma chave de assinatura e versionCode crescente. Antes de desinstalar um app Dev separado, exporte os saves e teste sua importação. Não distribua ROMs ou BIOS.

## Conta KL e backup

Crie a conta em [Conta KL](https://kl-gba-play.emilysousa65477.chatgpt.site/conta.html), guarde o usuário e a senha gerados e entre em Perfil no aplicativo Plus. A conta autentica acesso ao servidor, não identidade civil/e-mail. O conjunto Sii+ é desbloqueado no editor, em Conjuntos.

O backup salva **nome, Sii, diário e estatísticas**. Não inclui saves de jogos, ROMs, capas, skins ou configurações. O app tenta enviar ao pausar uma partida; use **Salvar perfil no site** e confira o último envio confirmado. Jogar funciona offline. Conflitos preservam o perfil local e exigem restauração explícita; não há mesclagem automática. Sem usuário/senha guardados, não existe recuperação de acesso nesta versão. Exclusão da conta e do backup é possível no site e no aplicativo.

## Multiplayer experimental

Salas locais comportam até 3 participantes, mas o cabo GBA Multi-Pak funciona com **2 jogadores**, em Android ARM64. Mario Kart: Super Circuit foi testado pelo criador. Pokémon Quetzal apresentou falha. RFU, multiplayer DS/3DS e compatibilidade geral com hacks não são garantidos. Avanço rápido e operações de save state ficam desativados durante o cabo.

## Compilar

Use JDK 17, Android SDK 35, CMake 3.22.1 e NDK 27.2.12479018. Clone este repositório na branch `dev` com submódulos recursivos. Prepare o runtime fixado antes do Gradle:

```sh
git clone --recurse-submodules https://github.com/Swordfish90/LibretroDroid kl-runtime-source
git -C kl-runtime-source checkout 0ebd299624bfd51a0a1336dd0a2c56fe7ddbc0e3
git -C kl-runtime-source submodule update --init --recursive
python3 tools/kl-runtime/prepare.py kl-runtime-source
./gradlew :lemuroid-app:assembleFreeBundleDebug
```

A rotina completa, incluindo o núcleo cabo GBA, está em `.github/workflows/build-klgba.yml`. Releases de produção exigem a chave privada original, mantida somente nos secrets do GitHub. Nunca publique a chave ou senha. As releases incluem SHA-256, certificado público e referência do código-fonte.

## Créditos e licença

Lemuroid e LibretroDroid: Filippo Scognamiglio / Swordfish90 e colaboradores. Núcleos libretro e bibliotecas: seus respectivos autores. Cabo GBA experimental: Aelvryx/mgba-wifi-link (commit fixado na rotina de build). KL Play é um fork independente, sem vínculo oficial com Nintendo ou os autores dos jogos.

Preserve a [licença GPL-3.0](COPYING) e as licenças dos componentes, presentes no projeto e nos submódulos. O código de cada APK é identificado na release e em `FONTE.txt`. Os patches do runtime estão em `tools/kl-runtime`, junto aos testes de limites de áudio. As imagens de jogos no site demonstram a interface; o aplicativo não contém jogos.
