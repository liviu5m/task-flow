# opencode Agent Instructions

## General Rules

1. **NO EDITING FILES WITHOUT EXPLICIT REQUEST**
   - Only create/modify test files unless user explicitly asks to edit
   - When user says "dont edit any file just here in chat" - NEVER edit files, only analyze and discuss

2. **ALWAYS READ MEMORY FIRST**
   - Check `~/code/brain/10-projects/task-flow/codebase-audit-and-fix-roadmap.md` for context
   - Check `steps.txt` for progress
   - Check git status for uncommitted changes

3. **BE CONCISE**
   - 1-3 sentences max
   - No emojis unless requested
   - No explanations unless asked
   - Direct answers only

4. **TEST FIRST**
   - Always create tests to reproduce bugs before fixing
   - Place tests in appropriate test directories
   - Use existing test patterns

5. **REMEMBER SESSION CONTEXT**
   - This file is read at the start of each session
   - Use it to maintain continuity across sessions

6. **LEARNING MODE**
   - When learning something new, ask clarifying questions to understand deeply
   - Ask for confirmation before assuming understanding

7. **SKILLS ACTIVATION**
   - Ensure caveman skills are loaded and active
   - Verify memory skills are accessible
   - Confirm all configured skills are operational at session start
