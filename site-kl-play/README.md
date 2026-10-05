# Site do KL Play — cópia independente

Código do site atualizado em 5 de outubro de 2026: HTML, CSS, JavaScript, fotos enviadas pelo criador e logo original do KL. Este repositório não contém senhas, tokens nem chave privada de assinatura.

A página permite tema claro/escuro, ampliação das fotos e comparação das skins. O download atual é KL Play 0.5.0-rc.1, SHA-256 `709511255f7dabbbfe26dbeac898a985780c94808af205190fadbecf18c0b825`.

`frontend/` contém a página e suas imagens. `worker/handler.js` contém o serviço de download usando um bucket R2 chamado logicamente `BUCKET`. `scripts/embed-assets.mjs` gera a entrada Worker com os recursos embutidos; `scripts/build.sh` prepara `dist/`. O manifesto de hospedagem deve ser fornecido pelo dono da nova hospedagem; não incluímos a identidade interna do serviço atual nesta cópia.

Esta cópia não depende de uma assinatura ChatGPT para ser lida ou modificada. Para migrar, um desenvolvedor pode adaptar o Worker ao novo provedor e armazenar o APK assinado no bucket, ou servir a página como site estático e apontar o botão para o download publicado na Uptodown. Não recrie chaves de assinatura se deseja atualizar por cima do aplicativo já instalado.

Site atual: https://kl-gba-play.emilysousa65477.chatgpt.site
Fontes do app: https://github.com/thaynansusyt-jpg/Lemuroid
Preserve a GPL-3.0 e as licenças próprias dos componentes. Jogos, ROMs e BIOS não fazem parte desta cópia; as imagens dos jogos aparecem apenas nas capturas fornecidas.
