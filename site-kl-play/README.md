# Site KL Play com contas e backup

Código da página e API de contas da atualização Plus. O backend utiliza Cloudflare Workers e D1; os APKs ficam em R2.

Instale dependências com npm ci. Gere apenas novas migrations com npx drizzle-kit generate e aplique o histórico de drizzle na base D1. Configure os bindings DB e BUCKET. Use bash scripts/build.sh para embutir a interface no Worker. Em outra hospedagem, ajuste a origem ACCOUNT_ORIGIN em worker/accounts.js e a URL do serviço no aplicativo KlCloudAccount.kt. Não publique dados da base nem credenciais dos usuários.

O perfil salvo contém nome, Sii, diário e estatísticas; saves e ROMs não são enviados. As senhas são segredos aleatórios de 256 bits gerados no servidor e guardados na base somente como hash. As sessões são aleatórias, expiram após 30 dias e têm hashes na base. Há isolamento por conta e controle de revisão para impedir sobrescrita por dispositivos com cópias antigas.

O app e o código não dependem de uma assinatura ChatGPT, mas a hospedagem e a base de dados precisam de manutenção. Antes de migrar a hospedagem, exporte a base D1 de forma privada; o GitHub contém o esquema, não os dados pessoais. Guarde a chave original de assinatura Android em local privado.

Os assets originais estão em frontend/assets. GPL-3.0 e créditos do Lemuroid e componentes preservados.
