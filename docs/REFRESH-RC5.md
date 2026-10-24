# KL Play 1.0.0-rc.5 — Refresh & DS

Candidata em desenvolvimento; não certificada em aparelho real.

## Mudanças
- Início com destaque para retomar o último jogo, filtros de console e carrosséis de capas carregados sob demanda.
- Paleta clara/escura renovada, rótulos de navegação visíveis e seções animadas nas configurações.
- Editor Sii com abas de roupa/aparência, quatro tons de pele e três estilos de cabelo; perfis antigos preservados.
- Importação de saves de bateria pelo menu ao segurar uma capa: .sav, .srm e .dsv; backup e arquivamento de estados automáticos anteriores. Compatibilidade estrutural não identifica a ROM: escolha o mesmo jogo/versão.
- Perfis DS para melonDS: Padrão do núcleo, Equilibrado, Desempenho e Compatibilidade.
- Multiplayer GBA retirado do lançamento de jogos e da compilação. Cabo GB/GBC mantido numa seção separada.
- Integração experimental DS WFC com núcleo melonDS DS separado, Kaeru e inicialização sem estados rápidos.
- GameCube adiado; nenhum console novo incluído.

## Teste no Android
1. Instalar a candidata com a mesma assinatura da versão anterior, sem desinstalar o KL.
2. Verificar biblioteca, capas, temas, controles e perfil/Sii antigos.
3. No DraStic, salvar no menu do Pokémon Black e fechar o emulador. Copiar/exportar o .dsv. No KL, segurar a capa do mesmo jogo, Importar save e confirmar; usar melonDS e Continuar dentro do Pokémon.
4. Confirmar que abrir o KL não restaura automaticamente um estado anterior ao save importado. Não carregar um estado manual antigo após importar.
5. Comparar os perfis DS na mesma cena, após reabrir o jogo; anotar velocidade, áudio e travamentos.
6. DS Online: habilitar, abrir jogo WFC, configurar melonAP no jogo e testar conexão. Depois testar uma troca/batalha real com Friend Codes, salvar, fechar e reabrir. Testar em dois aparelhos; conexão ao servidor não comprova multiplayer completo.
7. Desabilitar DS Online e verificar retorno ao núcleo habitual sem perda de save.

## Limites
- O núcleo online está previsto para ARM64; a opção fica indisponível quando a biblioteca não está incluída.
- WFC depende do jogo e dos serviços comunitários. Não implementa salas DS locais ou Download Play.
- Estados rápidos de DraStic não são portáveis; a importação usa saves de bateria.
- Saves DS importados são destinados ao melonDS, não ao DeSmuME antigo.
- Importação não cobre saves de cartão de memória nem pastas de saves de 3DS/PSP.
- Ganhos de desempenho e WFC precisam de validação no Galaxy A15 e em outro aparelho.

Fontes: https://docs.libretro.com/library/melonds_ds/ ; https://kaeru.world/projects/wfc ; https://github.com/JesseTG/melonds-ds
