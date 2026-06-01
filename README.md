# Presupuestos - MB Cerramientos

Aplicación Android para la gestión de presupuestos de cerramientos (ventanas, puertas y barandas). Permite crear presupuestos detallados con especificaciones técnicas, gestionar clientes y generar PDFs profesionales con diagramas técnicos.

## Funcionalidades

- **Presupuestos**: Crear, editar, duplicar y eliminar presupuestos con número identificador, cliente, proyecto y mano de obra
- **Items**: Agregar ventanas, puertas, barandas u otros con dimensiones (mm), cantidad de hojas, tipo de hoja (fijo/móvil), precio y notas
- **Clientes**: Gestión completa con buscador por nombre, teléfono, ciudad y email
- **Configuración**: Datos de la empresa, logo y términos y condiciones
- **Generación de PDF**:
  - Encabezado con logo y datos de la empresa
  - Detalle técnico con diagramas a escala por item
  - Tabla de items con precios y subtotales
  - Marca de agua del logo en cada página
- **Compartir**: Enviar el PDF por WhatsApp, email u otras apps

## Stack tecnológico

| Categoría | Tecnología |
|-----------|-----------|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| Arquitectura | MVVM + StateFlow |
| Base de datos | Room 2.7.0 |
| Generación de PDF | iText 8.0.3 |
| Navegación | Compose Navigation |
| Min SDK | 28 (Android 9) |
| Target SDK | 36 |

## Estructura del proyecto

```
app/src/main/java/com/example/myapplication/
├── data/
│   ├── db/             # Room database, entidades y DAOs
│   ├── repository/     # Repositorios de datos
│   └── service/        # PdfGeneratorService, TechnicalDiagramDrawer, SharingService
├── di/                 # Inyección de dependencias (AppContainer)
├── presentation/
│   ├── navigation/     # NavGraph
│   ├── ui/screen/      # Pantallas (Home, Presupuestos, Clientes, Configuración)
│   └── viewmodel/      # ViewModels por pantalla
└── utils/              # Utilidades de moneda y formato
```

## Base de datos

Room v3 con las siguientes tablas:

- **budgets**: Presupuestos (número, cliente, proyecto, mano de obra, estado)
- **budget_items**: Items del presupuesto (tipo, dimensiones, hojas, precio, tipo de hoja)
- **clients**: Clientes (nombre, CUIT, dirección, teléfono, email)
- **settings**: Configuración de la empresa (singleton)

## Build

```bash
# Debug
./gradlew assembleDebug

# Release (requiere keystore configurado)
./gradlew assembleRelease

# Limpiar
./gradlew clean
```

## Generar APK de release

1. Abrir en Android Studio
2. **Build → Generate Signed Bundle / APK → APK**
3. Seleccionar el keystore
4. Build variant: `release`
5. El APK queda en `app/release/app-release.apk`
