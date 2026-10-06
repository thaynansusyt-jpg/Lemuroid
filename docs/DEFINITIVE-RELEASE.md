# KL Play 1.0.0 — candidata da Definitive Edition

Este APK é uma candidata para testes, não a conclusão de todos os recursos planejados para a 1.0.0. Mantém o pacote e a assinatura das versões anteriores. Instale por cima; não precisa desinstalar nem apagar os dados.

## Correção de multiplayer da rc.4

- Corrigida a barreira de rede por quadro introduzida na rc.3 para Pokémon/Quetzal e adaptador sem fio GBA. Agora há uma janela limitada de três quadros, que tolera diferenças no ritmo das telas dos dois celulares sem exigir confirmação de cada quadro antes de avançar.
- Ao pausar um aparelho, o outro pode avançar até três quadros (cerca de 50 ms a 60 FPS) e então espera. A retomada não executa uma sequência de quadros acumulados. O primeiro quadro ainda exige que os dois aparelhos estejam conectados e proponham iniciar.
- Instale a rc.4 nos dois aparelhos e recrie a sala: o protocolo é KLR4. Mantém assinatura, saves e pacote. Multi-Pak mGBA e cabo GB/GBC não foram alterados.
- Teste de regressão usa duas instâncias do transporte com chamadas a cada 17 ms e diferença de fase de 9 ms, além dos testes de pausa, fragmentação e desconexão. Isso verifica o transporte; fluidez de Pokémon/Quetzal precisa de teste nos aparelhos físicos.

## Mudanças da rc.3

- Modos Pokémon/Quetzal e adaptador sem fio GBA agora esperam a confirmação do próximo quadro pelo outro aparelho. Uma pausa ou aparelho lento faz o par esperar, em vez de acumular movimentos para reproduzir depois. A espera inicial também evita abrir um jogo adiantado em um dos aparelhos. Ambos precisam usar a rc.3: protocolo atualizado para KLR3.
- Isso não promete eliminar toda a latência do Wi-Fi e não altera o Multi-Pak mGBA nem o cabo GB/GBC. Testes físicos de pausa, retomada e fluidez continuam necessários.
- Editor DS/3DS em tela inteira, sem sliders: arraste a imagem e puxe o ponto do canto inferior direito para redimensionar. Superior/inferior têm ajustes separados por orientação. No jogo, a prévia usa a imagem real e atualiza pela GPU durante a edição. Salvar ativa tela cheia para manter o espaço da prévia; cancelar restaura as opções anteriores. O jogo continua em execução durante a edição, com os controles virtuais escondidos.
- Em Configurações → Telas e controles, o editor ocupa uma tela própria com prévia esquemática. Gire o celular para editar a outra orientação.
- Configurações → Arquivos e saves → Escolher pasta dos saves: selecione/crie Documentos/KL Play pelo seletor do Android. Saves SRAM, estados, prévias e backups do cabo ganham cópias acessíveis fora de Android/data. Saves nativos na pasta do núcleo, incluindo Citra, são copiados ao sair ou colocar o jogo em segundo plano. Conteúdo instalado/DLCs e cache de shaders não fazem parte dessa cópia.
- Os núcleos mantêm arquivos de trabalho privados. Ao escolher uma pasta, os arquivos antigos são copiados para a área interna privada do app e preservados no local anterior. Falhas de espaço mantêm o caminho antigo utilizável. Não é uma mudança do Android para permitir File diretamente numa URI SAF.
- A pasta pública mantém a versão anterior como `.previous`; a nova cópia é verificada por SHA-256 antes de substituir a anterior. Falha de permissão/pasta não apaga o save privado. O status em Arquivos e saves mostra cópias pendentes.
- Reconectar uma pasta restaura arquivos que estejam faltando, sem substituir progresso já existente. Não é sincronização de nuvem nem importação automática de modificações feitas por fora quando já existe uma cópia local.
- Testes de migração preservam o progresso original e verificam retomada sem substituir saves mais novos. Transporte testado com pausa simulada, proposta fragmentada, ausência de quadros acumulados e contador inválido.

## Correções da rc.2

- Corrigida a exceção ao iniciar Pokémon/Quetzal: o seletor procurava gpSP na lista de núcleos singleplayer do GBA, que só registra mGBA.
- O carregador agora usa a biblioteca de cabo dedicada. Antes ele também procurava o arquivo antigo do gpSP, que não é incluído nesta versão.
- Teste de regressão usa a lista real de sistemas do app e verifica os três modos GBA e os caminhos GB/GBC.
- Editor das duas telas de DS/3DS: posição horizontal, vertical e largura independentes para superior/inferior, com prévia e ajustes separados por orientação. Em Configurações → Telas e controles → Editar posição e tamanho das duas telas. Salve e reabra o jogo.
- A composição usa GPU, preserva proporções e remapeia o toque para a tela inferior. O layout personalizado usa filtro simples; os modelos anteriores continuam disponíveis. Precisa de validação física das telas e do toque.

## Disponível nesta candidata

- Introdução com animação, cores do tema e adaptação para telas baixas/horizontais.
- Configurações organizadas em aparência, telas, desempenho e CPU emulada do 3DS.
- Cliques sonoros na navegação, biblioteca, atalhos e editor, com opção de desativar. Seguem os sons do Android.
- Corrigido o destino da edição da skin ao girar o celular com o editor aberto: salvar aplica à orientação em que a edição começou.
- Editor com modelos KL Azul, Contorno portátil, Retro, Sonic e Discreto; contornos até 6 dp.
- Importação de fontes TTF/OTF, até 4 MB e 32 fontes. Cópia privada preservada mesmo se o arquivo original for removido. Fontes e skins não entram no backup de perfil do site.
- Ajuste real de clock da CPU virtual do Citra: 50, 75, 100, 125, 150 ou 200%. Configuração do núcleo restaura o ajuste anterior. Não faz overclock do celular.
- Cabo experimental GB/GBC em dois aparelhos ARM64, com núcleo Gambatte separado do singleplayer.
- GBA mantém o Multi-Pak do Mario Kart e ganha dois modos gpSP separados: cabo Pokémon Gen3 e adaptador sem fio.
- Transporte com chave da sala, limites de fila e pacote, desconexão e proteção contra escrita em socket fechado. Modos gpSP diferentes são recusados na confirmação da sala.
- Corrigido no núcleo de cabo GB/GBC o deslocamento dos bits recebidos durante consultas parciais ao registrador serial.

## Como testar Pokémon e Quetzal

1. Nos dois celulares, em Multiplayer, ative cabo Wi-Fi e selecione o mesmo modo **antes de criar a sala**.
2. Para Mario Kart Super Circuit, use **Multi-Pak**. Para Pokémon comercial de GBA, teste **Pokémon • cabo**. Para Quetzal, use também **Pokémon • cabo**, conforme esclarecimento do mantenedor do gpSP em https://github.com/libretro/gpsp/issues/245. O modo **Adaptador sem fio** é para jogos que usem o adaptador wireless do GBA.
3. Crie a sala em um aparelho e conecte o segundo pelo IP. A sala de jogo deve ter exatamente duas pessoas.
4. Abra o jogo nos dois aparelhos e use a opção multiplayer dentro do jogo. Primeiro use a mesma edição/versão da ROM; depois teste combinações compatíveis.
5. GB/GBC escolhem automaticamente seu núcleo de cabo. Teste primeiro um jogo conhecido compatível e só depois hacks.
6. Se perder conexão, saia do jogo e refaça a sala. Avanço rápido e save states ficam desativados durante a sessão para preservar o tempo da ligação. Um backup local da SRAM anterior à sessão é guardado.

O responsável pelo projeto confirmou Quetzal funcionando na rc.2. As mudanças de sincronização da rc.3, trocas/batalhas e compatibilidade de outros aparelhos ainda precisam de testes físicos. O teste automatizado do GB verifica bytes em ROMs próprias, não equivale a validar todos os jogos.

## Ainda não entregue

- Multiplayer DS/3DS, incluindo serviços online da internet.
- PS2 e novos sistemas além dos já presentes.
- Instalação/download de DLCs e atualizações de jogos.
- Auditoria de todas as linhas e correção de todos os bugs de todos os núcleos.

Não há botões simulando esses recursos. A 1.0.0 final deve incluir somente funções que consigamos integrar e validar. A proposta de Switch foi retirada do trabalho atual.

## Verificação

A integração contínua compila o APK Android assinado e verifica pacote/versão. Testes nativos cobrem TCP fragmentado, chave da sala, pacote acima do limite, desconexão, prazo de leitura e SIGPIPE. Duas instâncias reais do Gambatte executam ROMs de teste próprias e trocam bytes pelo registrador serial emulado. O cabo mGBA mantém seu teste anterior com fixture aberta.

## Fontes e créditos

Lemuroid e seus autores; LibretroDroid; libretro/gambatte-libretro; libretro/gpsp; Aelvryx/mgba-wifi-link; Citra e autores dos demais núcleos. Fontes exatas do APK em FONTE.txt. Licenças originais preservadas; alterações nativas em tools/kl-gb, tools/kl-wireless e tools/kl-link.
