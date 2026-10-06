# Site KL Play — SEGA Edition, contas e backup

Código da página e API de contas da atualização 0.5.0s Plus. O backend utiliza Cloudflare Workers e D1; o download atual redireciona ao APK assinado da Release no GitHub. R2 mantém os arquivos legados.

Instale dependências com npm ci. Gere novas migrations com npx drizzle-kit generate e preserve o histórico de drizzle na base D1. Configure os bindings DB e BUCKET. Use bash scripts/build.sh e node scripts/validate-artifact.mjs para embutir e validar a interface no Worker. Execute node tests/accounts.mjs para verificar autenticação, isolamento, revisão e recompensas com SQLite real.

Em outra hospedagem, ajuste ACCOUNT_ORIGIN em worker/accounts.js e a URL do serviço em KlCloudAccount.kt. Atualize a constante PLUS em worker/handler.js somente depois de verificar versão, tamanho e SHA-256 do APK publicado. Não publique credenciais, tokens ou dados da base.

O backup contém nome, Sii, conquistas, diário e estatísticas; saves, ROMs, capas e skins não são enviados. Perfis anteriores continuam aceitos. A SEGA Edition inclui validação dos conjuntos Sonic/Super Sonic, medalhas, eventos e métricas de uso. As senhas e sessões aleatórias são guardadas somente como hashes. Há isolamento por conta e controle de revisão para impedir sobrescrita por dispositivos com cópias antigas.

O app e o código não dependem de uma assinatura ChatGPT, mas a hospedagem e a base de dados precisam de manutenção. Antes de migrar, exporte a base D1 de forma privada; o GitHub contém o esquema, não os dados pessoais. Preserve a chave original de assinatura Android em local privado.

Assets em frontend/assets. Não há novas capturas da SEGA Edition: a galeria identifica a interface Wii U. GPL-3.0 e créditos do Lemuroid e dos componentes preservados. Edição independente de fã, sem afiliação com SEGA ou Nintendo.
