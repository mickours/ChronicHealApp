# Project
We are implementing the Android app describe in the [./README.md]().
We follow the design described in [./docs/design.md]().
The implementation details are explained in [./docs/implementation.md]().

## Instructions
Read these files carefully.
Scan the code structure and propose the next step.
Stop when a feature is complete and propose a git commit message with details and that I can copy.

**IMPORTANT FOR THE AGENT**
follow these rules:
- every time you trigger an action explain me why in a concise way
- always run the gradle sync and gradle build to test your changes
- iterate until you have fix all build errors
- explain the technical choice you make in the [./docs/implementation.md]() file.
- do not use `import package.*` notation
- if you loop on actions stop and ask for guidance 
- when i ask a question, don't modify the code, just answer
- If you change data schema, make sure that you create a migration make some test to avoid any data loss
- When you add a entry in strings.xml, make sure that they are all translated