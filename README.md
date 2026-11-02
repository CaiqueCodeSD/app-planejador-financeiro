
<!-- ====================================================== -->
<!--                 PLANEJADOR FINANCEIRO                   -->
<!-- ====================================================== -->

<div align="center">

  <img src="docs/assets/banner.png" width="100%" alt="Planejador Financeiro Banner"/>

  <h1>Planejador Financeiro</h1>

  <p><strong>Seu dinheiro, mais organizado.</strong></p>

  <p>
    Aplicativo Android para organização, acompanhamento e controle
    das finanças pessoais.
  </p>

  <p>
    <a href="#-sobre-o-projeto">Português</a> •
    <a href="#-about-the-project">English</a>
  </p>

  <p>
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
    <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
    <img src="https://img.shields.io/badge/Room-101411?style=for-the-badge&logo=sqlite&logoColor=B7F36B" alt="Room"/>
    <img src="https://img.shields.io/badge/Status-Em%20desenvolvimento-B7F36B?style=for-the-badge" alt="Em desenvolvimento"/>
  </p>

</div>

---

## 🇧🇷 Sobre o projeto

O **Planejador Financeiro** é um aplicativo Android desenvolvido com o objetivo de facilitar a organização e o acompanhamento das finanças pessoais.

A proposta é oferecer uma experiência simples, intuitiva e visualmente agradável, permitindo que o usuário tenha maior clareza sobre suas receitas, despesas, contas e movimentações financeiras.

O projeto também busca priorizar a autonomia do usuário sobre seus próprios dados, utilizando persistência local e tendo como objetivo uma futura integração com o Google Sheets para sincronização das informações.

### 🎯 Objetivos

- Centralizar informações financeiras em um único aplicativo.
- Facilitar o acompanhamento de receitas e despesas.
- Organizar contas e movimentações financeiras.
- Oferecer uma experiência intuitiva e minimalista.
- Priorizar o controle e a autonomia sobre os dados financeiros.

## ✨ Funcionalidades

As funcionalidades são organizadas conforme o desenvolvimento do projeto.

- [x] Dashboard financeiro.
- [x] Cadastro e gerenciamento de contas.
- [x] Cadastro de receitas e despesas.
- [x] Histórico de movimentações financeiras.
- [x] Edição e exclusão de registros.
- [x] Navegação entre períodos mensais.
- [x] Fundação de persistência e sincronização local.
- [ ] Integração com Google Sheets API.
- [ ] Sincronização entre aplicativo e planilha.
- [ ] Aprimoramentos dos relatórios financeiros.

## 🛠️ Tecnologias

O projeto utiliza tecnologias do ecossistema Android e ferramentas voltadas à organização, persistência e gerenciamento de dados.

| Tecnologia | Aplicação |
|---|---|
| Kotlin | Linguagem principal |
| Android SDK | Desenvolvimento Android |
| Room | Persistência local |
| Koin | Injeção de dependências |
| Kotlin Flow | Fluxos reativos de dados |
| Android Studio | Ambiente de desenvolvimento |
| Gradle | Automação de build |

## 🏗️ Arquitetura

O projeto busca manter uma separação clara de responsabilidades, facilitando a manutenção, os testes e a evolução da aplicação.

A organização considera:

- **Activities:** responsáveis pela interface e interação com o usuário.
- **ViewModels:** gerenciamento de estado e lógica de apresentação.
- **Repositories:** abstração e organização do acesso aos dados.
- **Room:** persistência das informações localmente.
- **Koin:** gerenciamento de dependências.

A arquitetura é organizada para permitir a evolução da aplicação sem concentrar responsabilidades em componentes individuais.

## 📂 Estrutura do projeto

```text
app/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/example/planejadorfinanceiro/
    │   │       ├── data/
    │   │       ├── domain/
    │   │       ├── presentation/
    │   │       └── ...
    │   └── res/
    ├── test/
    └── androidTest/

docs/
├── architecture/
└── assets/
    ├── screenshots/
    ├── banner.png
    ├── logo.png
    └── icon.png
```

*Estrutura ilustrativa; será ajustada à organização real dos pacotes do projeto.*

## 🗺️ Roadmap

- [x] Construção da interface inicial.
- [x] Cadastro e gerenciamento de contas.
- [x] Gerenciamento de receitas e despesas.
- [x] Histórico de movimentações.
- [x] Navegação mensal.
- [ ] Consolidação da persistência e sincronização local.
- [ ] Integração com Google Sheets API.
- [ ] Sincronização bidirecional de dados.
- [ ] Aprimoramento dos relatórios e indicadores financeiros.

## 🚀 Como executar

### Pré-requisitos

- Android Studio.
- JDK compatível com a configuração do projeto.
- Android SDK.
- Git.

### Instalação

Clone o repositório:

```bash
git clone https://github.com/CaiqueCodeSD/app-planejador-financeiro.git
```

Entre no diretório:

```bash
cd app-planejador-financeiro
```

Abra o projeto no Android Studio, aguarde a sincronização do Gradle e execute a aplicação em um dispositivo ou emulador Android compatível.

---

## 🇺🇸 About the Project

**Planejador Financeiro** (Financial Planner) is an Android application designed to make personal financial organization and tracking easier.

The goal is to provide a simple, intuitive, and visually appealing experience, allowing users to manage their income, expenses, bills, and financial transactions.

The project also aims to prioritize user autonomy over personal financial data, using local persistence and planning a future integration with Google Sheets for data synchronization.

### 🎯 Goals

- Centralize financial information in one application.
- Make income and expense tracking easier.
- Organize bills and financial transactions.
- Provide an intuitive and minimalist experience.
- Prioritize user control and data autonomy.

## ✨ Features

- [x] Financial dashboard.
- [x] Bill registration and management.
- [x] Income and expense registration.
- [x] Financial transaction history.
- [x] Record editing and deletion.
- [x] Monthly navigation.
- [x] Local persistence and synchronization foundation.
- [ ] Google Sheets API integration.
- [ ] App-to-spreadsheet synchronization.
- [ ] Enhanced financial reports.

## 🛠️ Technologies

| Technology | Purpose |
|---|---|
| Kotlin | Main programming language |
| Android SDK | Android development |
| Room | Local persistence |
| Koin | Dependency injection |
| Kotlin Flow | Reactive data streams |
| Android Studio | Development environment |
| Gradle | Build automation |

## 🏗️ Architecture

The project follows a separation of responsibilities to improve maintainability, testing, and future development.

Its architecture considers:

- **Activities:** UI and user interaction.
- **ViewModels:** presentation logic and state management.
- **Repositories:** data access abstraction.
- **Room:** local data persistence.
- **Koin:** dependency management.

## 🗺️ Roadmap

- [x] Initial interface.
- [x] Bill management.
- [x] Income and expense management.
- [x] Transaction history.
- [x] Monthly navigation.
- [ ] Local persistence and synchronization consolidation.
- [ ] Google Sheets API integration.
- [ ] Bidirectional data synchronization.
- [ ] Enhanced financial reports.

## 🚀 Getting Started

### Requirements

- Android Studio.
- JDK compatible with the project configuration.
- Android SDK.
- Git.

### Installation

Clone the repository:

```bash
git clone https://github.com/CaiqueCodeSD/app-planejador-financeiro.git
```

Navigate to the project directory:

```bash
cd app-planejador-financeiro
```

Open the project in Android Studio, wait for Gradle synchronization, and run the application on a compatible Android device or emulator.

---

<div align="center">

  <p><strong>Planejador Financeiro</strong></p>

  <p>Developed with Kotlin and a focus on financial organization.</p>

  <p>
    <sub>Personal Finance · Android · Open Source Portfolio Project</sub>
  </p>

</div>
