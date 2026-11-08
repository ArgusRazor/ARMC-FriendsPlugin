# ARMC - Friends Plugin

> Plugin moderno, ligero y modular de **Amigos y Grupos (Party)** para servidores de Minecraft Paper y Spigot, optimizado para Minecraft 26.3 con soporte retrocompatible.

---

## Características Principales

- **Sistema de Amigos**:
  - Solicitudes de amistad con tiempo de expiración configurable.
  - Mensajería privada entre amigos (`/friend msg`).
  - Notificaciones de conexión y desconexión en chat y con sonidos.
  - Lista de amigos en chat y en interfaz gráfica.

- **Sistema de Grupos (Party)**:
  - Creación de grupos, invitaciones, expulsión y transferencia de liderazgo.
  - Chat interno de grupo (`/party chat` o `/p <mensaje>`).
  - Límite de miembros configurable (por defecto 6).

- **Interfaces Gráficas (GUI)**:
  - Menú interactivo de amigos con cabezas y skins de jugadores vía API oficial (`PlayerProfile`).
  - Menú de gestión de grupo.
  - Menú de ajustes de privacidad (activar/desactivar solicitudes, mensajes y notificaciones).

- **Almacenamiento y Rendimiento**:
  - Soporte para **SQLite** (local sin configuración externa) y **MySQL/MariaDB** (con pool de conexiones HikariCP).
  - Compatible con el esquema de base de datos de PartyAndFriends (`fr_players`, `fr_friend_assignment`, etc.).

- **Integraciones**:
  - Expansión para **PlaceholderAPI** (`%argusfriends_count%`, `%argusfriends_online%`, `%argusfriends_in_party%`, `%argusfriends_party_size%`, `%argusfriends_party_leader%`).

---

## Comandos Principales

| Comando | Alias | Descripción |
| :--- | :--- | :--- |
| `/friend` | `/f`, `/amigo` | Abre el menú visual o gestiona amigos (`add`, `remove`, `list`, `msg`, etc.). |
| `/friendsgui` | `/fgui` | Acceso directo a la interfaz gráfica de amigos. |
| `/party` | `/p`, `/grupo` | Gestiona el grupo (`invite`, `accept`, `deny`, `kick`, `leave`, `chat`, etc.). |
| `/partygui` | `/pgui` | Acceso directo a la interfaz gráfica de grupo. |
| `/argusfriends` | `/af` | Comando administrativo (`reload`, `info`, `version`). Requiere `argusfriends.admin`. |

---

## Requisitos y Compilación

- **Java**: 21 o superior.
- **Servidor**: Paper / Spigot 1.20+ hasta 26.3+.
- **Compilación**:
  ```bash
  mvn clean package
  ```
  El artefacto resultante se genera en `target/ArgusRazorFriends-1.0.0-MC26.3.jar`.

---


© 2026 Joqueselo. Todos los derechos reservados.
