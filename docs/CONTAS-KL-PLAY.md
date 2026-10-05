# Contas no KL Play

O perfil sem conta, o diário e o Sii já funcionam. O login só aparece ativo quando os identificadores públicos estão cadastrados em `config/kl-auth.properties`. Não coloque senha nem Client Secret nesse arquivo.

## GitHub (mais simples)

1. Entre no GitHub e abra https://github.com/settings/developers.
2. Em **OAuth Apps**, clique em **New OAuth App**.
3. Nome: **KL Play**. Homepage: a página oficial do KL ou `https://github.com/thaynansusyt-jpg/Lemuroid`.
4. Callback: a mesma URL da página oficial. O KL usa Device Flow, que não usa esse retorno.
5. Cadastre o aplicativo, abra suas configurações e ative **Enable Device Flow**.
6. Copie **Client ID**. Não gere nem envie Client Secret.
7. Em `config/kl-auth.properties`, coloque o ID depois de `github.clientId=`. Compile uma nova versão.

Na aba Perfil, a pessoa copia o código e abre a página oficial do GitHub. Nenhuma senha passa pelo KL. O aplicativo consulta somente a identidade pública da conta, sem pedir acesso aos repositórios.

## Google

1. Crie um projeto em https://console.cloud.google.com/ e configure **Google Auth Platform**: nome KL Play, e-mail de suporte, audiência e contatos.
2. Crie um cliente OAuth **Android** com pacote `com.klgames.klgba` e a impressão digital **SHA-1** da assinatura de lançamento. O arquivo `ASSINATURA-PUBLICA.txt` acompanha o APK no artefato da compilação. É uma impressão digital pública, não a chave privada.
3. Crie também um cliente OAuth **Aplicativo da Web** no mesmo projeto. Esse é o Client ID usado pelo login no aplicativo.
4. Coloque o ID do cliente Web depois de `google.webClientId=`. Não coloque Client Secret no código.
5. Enquanto a configuração estiver em teste, adicione seus usuários de teste. Para divulgação, conclua a configuração de produção e informe uma política de privacidade verdadeira.
6. Compile outra versão e teste o APK assinado em um aparelho com Google Play Services. A versão Dev tem pacote e assinatura diferentes e precisa de um cliente Android separado.

O Google confirma a identidade e o KL verifica assinatura, destinatário, validade e nonce da credencial. Credenciais ficam apenas em memória.

## O que a conta faz nesta versão

- Identifica o perfil por Google ou GitHub; esses provedores mantêm perfis separados.
- Na primeira conexão, copia o diário e o Sii do perfil sem conta para aquela conta, uma única vez.
- Nome, ID, Sii, diário e sequência ficam salvos **neste aparelho**. Não há sincronização de saves ou diário entre celulares.
- Cada dia com pelo menos um minuto jogado conta para a sequência. O tempo usa o relógio monotônico, começa após o primeiro quadro e para quando a atividade do jogo é pausada.
- Notas são editadas e salvas pelo usuário. Apagar o perfil local não apaga jogos nem saves.

## Multiplayer

O cabo Wi-Fi experimental continua limitado a dois jogadores no modo Multi-Pak. ROMs e hacks podem usar outros modos de comunicação. Esta atualização silencia o áudio após uma falha reportada pelo cabo, mas **não declara Pokémon Quetzal compatível**. Precisamos da versão exata da ROM e da mensagem exibida para investigar o protocolo utilizado.
