# Claude Code Project Instructions

## Primary Rule: SESSION_CONTEXT.md is Project Memory

**ALWAYS follow this pattern:**
1. **READ** SESSION_CONTEXT.md at session start
2. **REFERENCE** relevant sections during work
3. **UPDATE** SESSION_CONTEXT.md with findings and progress
4. **MAINTAIN** chronological investigation records

## Session Context Management

### When to Read SESSION_CONTEXT.md:
- Beginning of any new conversation
- Before starting investigations or debugging
- When user asks about current status or previous work
- When encountering errors or issues

### When to Update SESSION_CONTEXT.md:
- Starting new investigations
- Discovering root causes
- Implementing fixes or solutions
- Completing tasks or milestones
- Finding new related issues
- Making significant progress

### Required Documentation Format:
```markdown
## Current Status
- **Date**: [Current date]
- **Last Activity**: [What you're working on]

## [Issue/Task Name] - [Status Indicator]
### Analysis Results ✅ COMPLETED / 🔄 IN_PROGRESS / ❌ FAILED
- **Root Cause**: [Detailed explanation]
- **Files Analyzed**: [List with line numbers]
- **Implementation**: [What was changed]
- **Expected Outcome**: [What should be fixed]

### Status Tracking
- [x] Investigation completed
- [x] Root cause identified
- [x] Solution implemented
- [ ] Testing pending
```

## File Reference Standards
- Always include file paths and line numbers: `filename.java:123`
- Reference specific code sections when discussing changes
- Document both successful and failed approaches
- Maintain links between related issues

## Continuity Requirements
- Build upon previous investigations rather than restarting
- Acknowledge what has been tried and what worked/failed
- Continue investigation threads across sessions
- Maintain awareness of project timeline and status

**Remember**: SESSION_CONTEXT.md ensures no work is lost and every session continues from where the last one ended.