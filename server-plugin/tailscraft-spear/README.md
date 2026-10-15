# TailsCraft Spear plugin

This plugin targets Paper/Spigot 1.12.2 and makes the client spear work on a
server without adding a custom network item ID. Each spear is a normal sword
with a distinctive display name, which the TailsCraft client recognizes.

Build it with:

```sh
./build.sh
```

Install `build/TailsCraftSpear.jar` in the server's `plugins/` directory and
restart the server. Operators can use `/spear [wooden|stone|copper|iron|golden|diamond|netherite]`.

The plugin handles jab damage and validates the client's charge packet against
the held spear, target distance, line of sight, and relative movement.
