# Project
**Read these files and then read the instructions**

We are implementing the Android app describe in the [README](./README.md).
We follow the design described in the [design](./docs/design.md) document.
The implementation details are explained in the [implementation](./docs/implementation.md).

## Instructions
Stop when a feature is complete and propose a git commit message with details and that I can copy.

### Coding process

#### Start a new feature
- Scan the code structure and propose the next step.
- explain the technical choice you make in the [implementation](./docs/implementation.md) file.
- every time you trigger an action explain why, in a concise way
- always run the gradle sync and gradle build to test your changes
- iterate until you have fix all build errors
- if you have changed your plan, update the [implementation](./docs/implementation.md) file.

#### General coding rules

follow these general rules:
- if you loop on actions (e.g. read, edit, read, edit on the same file): stop and ask for guidance 
- when somebody ask you a question, don't modify the code, just answer
- If you change data schema, make sure that you create a migration and make some test to avoid any
  data loss
- When you add a entry in strings.xml, make sure that they are all translated and spell correctly in
  other languages
- do not use `import package.*` notation, use full import and remove unused imports
- avoid Experimental or Deprecated features if there is a simple alternative
- Avoid leaving unused imports and variables
- Fix warnings of the build unless you have a good reason not to