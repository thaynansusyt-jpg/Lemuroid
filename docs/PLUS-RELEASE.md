# KL Play 0.5.0 Plus

Candidata de manutenção — instale por cima de 0.5.0-rc.1. Android 6.0+, pacote e assinatura preservados.

## Correções
- Áudio do avanço rápido: callbacks são divididos em blocos com limite de memória; proteção de buffer, silêncio em underruns e guarda contra entradas vazias no resampler.
- Troca de velocidade sincronizada com o núcleo; valores limitados a 1–8x.
- O estado “carregando” é limpo também em falhas e retornos antecipados.

## Desempenho
- Núcleos de software enviam somente o último quadro acelerado à tela, mantendo a execução dos quadros do jogo.
- Orçamento de trabalho durante avanço rápido evita tentar todos os quadros quando o núcleo está lento. A velocidade solicitada não é uma garantia de velocidade atingida.
- A reprodução de áudio acompanha a quantidade efetiva de quadros executados.
- Os mesmos limites de memória se aplicam aos núcleos GB/GBC/GBA/SNES/DS/3DS/N64; não houve substituição geral dos núcleos, nem comprovação de melhora em todos os aparelhos.

## Conta e Sii+
- Conta KL criada no site com credenciais aleatórias; senha não é armazenada pelo aplicativo.
- Sessão protegida pelo Android Keystore. Backup de nome, Sii, diário e estatísticas; saves/ROMs não são enviados.
- Envio manual, tentativa de envio ao pausar, restauração explícita e exclusão da conta. Conflitos preservam o perfil local.
- Conjunto humano Sii+ na categoria Conjuntos do editor.

## Limites
Google/GitHub seguem em preparação. Cabo GBA experimental para 2 jogadores ARM64. Pokémon Quetzal ainda não foi corrigido. Nenhuma garantia universal de 8x/60 FPS. Teste público em dispositivos reais ainda é necessário.
