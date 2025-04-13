# Protocolo

## 1. Iniciar sesión (síncrono)

**1.1** Cliente al servidor

```json
{
    "username": <username>,
    "command": "login",
    "password": <password>
}
```

**1.2** Servidor al cliente

Si el inicio de sesión es exitoso

```json
{
    "username": "server",
    "command": "ok"
    "mapWidth": <map width in tiles>,
    "mapHeight": <map height in tiles>,
    "screenWidth": <screen width in pixels>,
    "screenHeight": <screen height in pixels>,
    "playerSpeed": <player speed>,
    "projectileSpeed": <projectile speed>,
    "map": <string with the map, nums 0 to 9>
    "spawnX", <spawn x coord>
    "spawnY", <spawn y coord>
}
```

Si el inicio de sesión falla

```json
{
    "username": "server",
    "command": "error",
    "message": <error message>
}
```

## 2. Comunicación durante el juego (asíncrono)

Todos los mensajes tienen el siguiente formato:

```json
{
    "username": <username>,
    "command": <command>
}
```

Dependiendo del comando puede tener más campos. Más adelante se explican estos
campos.

- up
- down
- left
- right
- shoot
- exit
- died
- spawn
- status

Cuando un cliente acaba de iniciar sesión, este debe enviar el siguiente
mensaje al servidor.

Este mensaje tiene el propósito de avisar a los clientes que ya se encuentran
conectados que deberían instanciar un nuevo jugador, o que un jugador acaba de
revivir.

```json
{
    "username": <username>,
    "command": "spawn",
    "x": <x coords>,
    "y": <y coords>,
    "numLives": <num lives>,
    "facing": <"left", "right", "up", "down">
}
```

Cuando esto sucede, todos los clientes deben responder con su status:

```json
{
    "username", <username>,
    "command": "status",
    "x": <x coords>,
    "y", <y coords>,
    "numLives": <num lives>
    "facing": <"left", "right", "up", "down">
}
```

Resumen de los comandos válidos durante la comunicación asíncrona:

Otro comando especial es `died`.

```json
{
    "username", <username>,
    "command", "died",
    "killer", <killer username>,
```
