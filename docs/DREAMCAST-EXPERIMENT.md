# KL Play: Dreamcast experimental e avaliação de Wii U

Data da avaliação: 6 de outubro de 2026. Base: KL Play 1.0.0-rc.3.

## Wii U: conclusão da integração atual

Há dois caminhos reais de Cemu para Android:

- Port experimental independente: https://github.com/SSimco/Cemu/tree/android-port
- Núcleo libretro ARM64: https://github.com/WizzardSK/cemu-libretro/tree/libretro

Núcleo avaliado: `fca53e35b8dade23a6370da6ef177c89844cc695`.
O arquivo `src/libretro/CemuLibretro.cpp` pede Vulkan 1.1 com negociação de dispositivo ou OpenGL de desktop Core 4.5 (fallback 4.1). O LibretroDroid do KL oferece um contexto EGL OpenGL ES 3. Não oferece dispositivo Vulkan, sincronização e apresentação de imagens Vulkan nem contexto de OpenGL desktop. Portanto, copiar a biblioteca Cemu não produz Wii U funcional.

O teste `tools/kl-runtime/test-hw-context.cpp` verifica a incompatibilidade dos tipos de contexto e a proteção de negociação. Isso é uma verificação de contrato gráfico, não uma execução de jogo de Wii U. Nenhum suporte a Wii U foi anunciado ou inserido no menu nesta versão.

Os 4 GB mínimos em https://cemu.info/ são requisitos da versão para PC. Não estabelecem desempenho no Galaxy A15. Para retomar Wii U, será necessário implementar e testar um backend Vulkan no frontend e depois medir consumo de RAM, drivers Mali, áudio, controles e TV/GamePad em aparelhos reais.

## Novo console: Sega Dreamcast

Motor: https://github.com/flyinghead/flycast/tree/5aa091fde632fb332c8d8c34e280d62dc951954c
Versão fixada: Flycast v2.7, commit `5aa091fde632fb332c8d8c34e280d62dc951954c`.
Licença: GPL-2.0 ou posterior; licença original incluída no APK e na release. Dependências e seus textos de licença estão preservados no código fixado de Flycast. Compilação reproduzível: `tools/kl-dreamcast/build-android.sh` e workflow do KL. Não são distribuídos jogos nem BIOS proprietárias.

Esta integração é experimental, somente Android ARM64. OpenGL ES, resolução original 640×480, renderização na mesma thread do contexto, VMUs separados por jogo e incluídos nos saves do KL. Controles: direcional, analógico esquerdo, A/B/X/Y, Start e gatilhos L2/R2. O menu do jogo permite 320×240, 640×480 e 1280×960, DSP e pular quadros. O desempenho depende do jogo e do aparelho.

### Testar

1. Instale o APK experimental por cima do KL atual, sem desinstalar. Mantenha backup dos saves.
2. Coloque seus arquivos Dreamcast em uma pasta chamada `dreamcast` dentro da biblioteca selecionada. Esse nome evita confundir CHD e M3U com PlayStation ou outros consoles.
3. Formatos: CDI, CHD, GDI com todas as faixas na mesma pasta, ou M3U que referencia seus discos. Para GDI, os nomes de faixas devem corresponder exatamente ao descritor.
4. Abra o jogo. Como o KL tem VFS versão 2 e Flycast usa versão 3, esta primeira versão usa arquivos reais em cache. No primeiro carregamento pelo seletor de pasta do Android, o jogo e as faixas serão copiados: pode demorar e exige espaço disponível equivalente ao jogo.
5. Se estiver lento, abra as opções do jogo e tente 320×240 antes de aumentar a resolução. Não há promessa de 60 FPS em todos os jogos.
6. Salve dentro do jogo, feche e reabra para verificar o VMU. Se você escolheu a pasta pública de saves no KL, o cartão por jogo será exportado para ela.

Flycast inclui BIOS HLE. Alguns jogos precisam da BIOS original obtida pelo usuário; a pasta usada pelo núcleo é `system/dc` dentro do diretório de sistema do KL. Não redistribuir BIOS. A configuração de BIOS emulada requer reiniciar o jogo.

Esta versão não acrescenta multiplayer de Dreamcast, Naomi ou Atomiswave. O cabo GBA/GB/GBC e o editor de telas DS/3DS permanecem na base rc.3.
