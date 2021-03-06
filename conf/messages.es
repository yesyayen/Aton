# Application
app.name=Aton Web
app.information=Administrador web de salas de computadoras. Desarrollado por Camilo A. Sampedro.
# Computer
computer.ip=Dirección IP / Hostname
computer.SSHUser=Usuario SSH
computer.SSHPassword=Contraseña SSH
computer.sendCommand=Enviar comando
computer.add=Agregar equipo
computer.executeCommandTitle=Ejecutar comando
computer.executeCommandBody=Ejecutar un comando SSH dentro del equipo
computer.shutdown=Apagar
computer.upgrade=Actualizar
computer.upgrade.succeeded=Actualización completada
computer.upgrade.succeeded.body=¡Actualización completada con éxito!
computer.unfreeze=Descongelar
computer.state=Estado:
computer.occupied=Ocupado
computer.available=Disponible
computer.error=Error
computer.addNew.header=Agregar computador
computer.notconnected=No conectado
computer.messageplaceholder=Mensaje
computer.sendmessage=Enviar mensaje
page.urlplaceholder=URL página
page.block=Bloquear página
computer.help.head=Ingresar computardores
computer.help.body=Aquí podrás ingresar computadores a una sala. \nCada computador necesita una dirección IP \
  (O host name), un nombre de usuario SSH y una contraseña del usuario SSH. Estos campos se requieren debido a que Aton\
  se comunica por SSH a cada uno de los computadores para ejecutar órdenes remotas y verificar su estado.\n\n\
  Es posible insertar cada uno de los equipos, separando con comas sus direcciones IP y sus nombres.
# Header texts
header.editUser=Editar usuario
# Menu texts
menu.suggestions=Sugerencias
menu.administration=Administración
menu.laboratories=Laboratorios
menu.sshorders=Órdenes SSH
menu.about=Acerca de
# Room panel
room.name=Nombre
room.audiovisualResources=Recursos audiovisuales
room.basicTools=Herramientas básicas
room.laboratoryID=Laboratorio
room.delete=Eliminar
room.empty.title=Aquí no hay computadores
room.empty.body=Parece que no se han encontrado computadores.
room.edit=Editar
room.notFound=Sala no encontrada con el id especificado
room.add=Agregar sala
room.help.head=Ingresar una nueva sala
room.help.body=Aquí podrás ingresar una nueva sala al sistema. Las salas son conjuntos de computadoras.
# Suggestion
suggestion.notImplemented=El módulo de sugerencias no está implementado... aún
# General
close=Cerrar
edit=Editar
delete=Eliminar
empty=Vacío
notImplemented=No implementado
contactAdmin=Por favor ayúdanos en sugerencias reportando este problema. Si eres administrador del laboratorio, por favor autentícate en la parte superior.
submit=Agregar
send=Enviar
about=Acerca de
# Laboratory
laboratory=laboratorio
laboratory.title=Laboratorio {0}
laboratory.laboratoryListTitle=Lista de laboratorios
laboratory.addButton=Agregar laboratorio
laboratory.addRoom=Agregar sala
laboratory.list.empty.body=Parece que no se han encontrado laboratorios.
laboratory.list.empty.title=Aquí no hay laboratorios
laboratory.list.empty.adminMessage=Si deseas puedes agregar salas desde el siguiente botón:
laboratory.empty.title=Aquí no hay salas
laboratory.empty.text=Parece que no se han encontrado salas.
laboratory.location=Ubicación:
laboratory.administration=Administración:
laboratory.name=Nombre:
laboratory.help.head=Ingresar un nuevo laboratorio
laboratory.help.body=Aquí podrás ingresar un nuevo laboratorio al sistema. Los laboratorios son conjuntos de salas que se encuentran reunidos en el mismo espacio.
# User
user.username=Nombre de usuario
user.notLoggedIn=No has iniciado sesión
user.login=Iniciar sesión
user.loginFormTitle=Inicia sesión en el sistema
user.goToHome=Volver a la página de inicio
user.connectedusers=Usuarios conectados:
# SSH Order
sshorders=Órdenes SSH
sshorder.command=Comando
sshorder.sentdatetime=Fecha de envío
sshorder.superuser=Como super usuario
sshorder.webuser=Usuario
sshorder.resulttext=Pero el resultado de ejecutar el comando fue "{0}" con código de salida {1}
# Suggestion
suggestions=Sugerencias
suggestion.add=Agregar sugerencia
suggestion.suggestionText=Sugerencia
suggestion.text=Sugerencia
suggestion.registereddate=Fecha de registro
suggestion.username=Usuario
suggestion.help.head=Enviar nueva sugerencia
suggestion.help.body=Envía al administrador sugerencias acerca de la sala, por ejemplo programas que quieras que se \
  instalen o alguna queja sobre algún proceso.