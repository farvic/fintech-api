Repositório do fintech-api.

A ser atualizado

## Configurando o JWT_SECRET

Por ser informação sensível, o JWT secret configurado no [src/main/resources/application.yml](src/main/resources/application.yml) não deve ficar exposto. 

```yml
security:
  jwt:
    issuer: fintech-api
    expiration-seconds: 3600
    # Set an environment variable named SECURITY_JWT_SECRET with a strong secret value
    secret: ${SECURITY_JWT_SECRET}
```

Foi utilizada uma variável de ambiente com uma chave no formato Base64 por questões de segurança. Para criá-la, insira o seguinte comando no prompt de comando do seu sistema operacional:

### No Windows

```powershell
$env:SECURITY_JWT_SECRET="SUA_CHAVE_EM_BASE64"
```

### No Linux/MacOS
```bash
export SECURITY_JWT_SECRET="SUA_CHAVE_EM_BASE64"
```

É possível gerar uma chave em base64 no site a seguir: [Random Base64 Generator][https://www.convertsimple.com/random-base64-generator/]
