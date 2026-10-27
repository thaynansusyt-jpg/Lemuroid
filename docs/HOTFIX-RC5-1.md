# KL Play 1.0.0-rc.5.1 — abertura de jogos

## Problemas identificados
- GameInteractor recusava abrir ou reiniciar jogos sem POST_NOTIFICATIONS em Android 13+. Essa permissão controla a exibição de notificações e não é pré-requisito para iniciar o serviço de jogo em primeiro plano.
- A preparação dos arquivos Dolphin ocorria antes da tela de jogo, sem indicação de progresso e sem bloquear novos toques.
- Falhas de pré-abertura eram informadas apenas por toast. O retorno RESULT_CANCELED do processo de jogo era ignorado.

## Correção
- Permissão de notificações deixa de bloquear abertura/reinício.
- Uma preparação por vez, com progresso visível na biblioteca.
- Falhas mostram mensagem persistente, com opção Copiar erro.
- Checkpoints de abertura são gravados atomicamente e podem ser lidos pelo processo principal após encerramento inesperado do processo de jogo.
- RVZ continua direcionado ao núcleo Dolphin; nenhuma conversão para ISO é exigida.

## Instalação e teste
Instalar esta versão por cima do KL Play Dev anterior, sem desinstalar. O pacote continua com.klgames.klgba.debug, com a mesma assinatura de teste; os dados da versão Dev são preservados.

Tocar uma vez em Pikmin RVZ e aguardar a preparação. Caso apareça erro, copiar a mensagem completa. O código e os testes identificam defeitos no fluxo de abertura; não comprovam que o dump de Pikmin do aparelho é válido ou que ele foi executado com sucesso neste ambiente.

Fontes: https://developer.android.com/develop/ui/views/notifications/notification-permission ; https://docs.libretro.com/library/dolphin/

## Validação
Build Android completo e testes Gradle passaram: 18 testes, zero falhas. Assinatura igual à candidata Dev anterior, pacote e versionCode conferidos; ZIP e presença do núcleo Dolphin e recursos Sys verificados. Execução de Pikmin no Galaxy A15 permanece pendente.
