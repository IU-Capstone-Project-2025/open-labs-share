# Contributing to Open Labs Share

This document provides guidelines and instructions for contributing to the project. Read full version of this document before starting to contribute

## Development Process

1. **Branch Management**
   - Style: start with lower case, connect with dashes (example: `kebab-case-example)
   - Main branch: `main`
   - Development branch: `dev`
   - Feature branches: `(num-of-issue)-(short-description)` (example: `09-fix-allignment`)

2. **Commit Guidelines**
   - Use clear and descriptive commit messages
   - Follow the format: `type(scope): description`
   - Types: feat, fix, docs, style, refactor, test, chore

3. **Pull Request Process**
   - Create PRs against the `dev` branch
   - Ensure all tests pass
   - Update documentation as needed
   - Get at least two reviews before merging

## Code Style

- Follow the existing code style
- Use meaningful variable and function names
- Keep functions small and focused


## Documentation

- Update README.md in worknig folder (not in the core folder) when adding new features
- Keep documentation up to date with code changes


## Security

- Never commit sensitive information
- Follow security best practices


## Commit Message Guidelines
### Commit Message Format
Each commit message consists of a **header**, a **body** and a **footer**.  The header has a special
format that includes a **type**, a **scope** and a **subject**:

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
```

The **header** is mandatory and the **scope** of the header is optional (read below).

Examples of correct commit message:

*More preferable:*
```
docs(changelog): update changelog to beta.5
```
*Acceptable:*
```
fix(release): need to depend on latest rxjs and zone.js

The version in our package.json gets copied to the one we publish, and users need the latest of these.
```



### Type
Must be one of the following:

* **deploy**: Changes that affect deploy of the system or additional services
* **ci**: Changes to our CI configuration files and scripts
* **docs**: Documentation only changes
* **feat**: A new feature
* **update**: Updating already introduced feature *(**scope** is mandatory)*
* **fix**: A bug fix
* **perf**: A code change that improves performance
* **refactor**: A code change that neither fixes a bug nor adds a feature
* **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)
* **test**: Adding missing tests or correcting existing tests

### Scope

The following is the current list of supported scopes (may be updated later or you can use logic here):


* **api**
* **users**
* **karma**
* **labs**
* **papers**
* **feedback**
* **questionary**
* **storage**
* **ml**
* **frontend**
* **docker**
* **k8s**


There are currently only one exception to the "use package name" rule:
* none/empty string: useful for `style`, `test` and `refactor` changes that are done across all packages (correct e.g. `style: add missing semicolons`)

### Subject
The subject contains a succinct description of the change:

* use the imperative, present tense: "change" not "changed" nor "changes"
* don't capitalize the first letter
* no dot (.) at the end

### Body
It is optional. Just as in the **subject**, use the imperative, present tense: "change" not "changed" nor "changes".
The body should include the motivation for the change and contrast this with previous behavior.

### Revert
If the commit reverts a previous commit, it should begin with `revert: `, followed by the header of the reverted commit. In the body it should say: `This reverts commit <hash>.`, where the hash is the SHA of the commit being reverted.