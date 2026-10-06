# KL Play 1.0.0 — candidata da Definitive Edition

Este APK é uma candidata para testes, não a conclusão de todos os recursos planejados para a 1.0.0. Mantém o pacote e a assinatura das versões anteriores. Instale por cima; não precisa desinstalar nem apagar os dados.

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
2. Para Mario Kart Super Circuit, use **Multi-Pak**. Para Pokémon comercial de GBA, teste **Pokémon • cabo**. Para uma versão de Quetzal que use adaptador sem fio, teste **Adaptador sem fio**.
3. Crie a sala em um aparelho e conecte o segundo pelo IP. A sala de jogo deve ter exatamente duas pessoas.
4. Abra o jogo nos dois aparelhos e use a opção multiplayer dentro do jogo. Primeiro use a mesma edição/versão da ROM; depois teste combinações compatíveis.
5. GB/GBC escolhem automaticamente seu núcleo de cabo. Teste primeiro um jogo conhecido compatível e só depois hacks.
6. Se perder conexão, saia do jogo e refaça a sala. Avanço rápido e save states ficam desativados durante a sessão para preservar o tempo da ligação. Um backup local da SRAM anterior à sessão é guardado.

Quetzal, trocas/batalhas de Pokémon e compatibilidade dos aparelhos ainda precisam de testes físicos. O teste automatizado do GB verifica bytes em ROMs próprias, não equivale a validar todos os jogos.

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
