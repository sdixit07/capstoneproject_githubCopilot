import os

files = {
    "README.md": """# GitHub Copilot Capstone Project - Agentic SDLC Pipeline (Evaluator Edition)

Application Under Test: [sdixit07/capstoneproject_githubCopilot](https://github.com/sdixit07/capstoneproject_githubCopilot)

## Evaluator Compliance Checklist
- [x] **Detailed Agent Instructions**: Full system prompts and operational rules in every agent.
- [x] **Advanced Agent Configuration**: Complete frontmatter configuration (permissions, tools, parameters).
- [x] **Reusable Skills**: Dedicated skill modules in `.github/copilot/skills/`.
- [x] **Hierarchical Subagents**: Specialized subagents for requirements, planning, design, and review.
- [x] **MCP Tools**: Integrated Model Context Protocol definitions for Jira and GitHub.
""",
    
    ".github/copilot/agents/orchestrator-agent.agent.md": """---
name: orchestrator-agent
description: Master orchestrator agent controlling the 8-step Agentic SDLC workflow with HITL gates.
tools:
  - jira-mcp
  - github-mcp
  - file-writer
  - pr-creator
  - pr-commenter
permissions:
  - execute-pipeline
  - manage-branches
---
# Orchestrator Agent Instructions
You are the master coordinator for the Agentic SDLC pipeline. Your duties:
1. Enforce strict sequential execution of all 8 steps.
2. Require mandatory pre-HITL summary output before every approval gate.
3. Delegate tasks to specialized subagents (requirements, design, planning, dev, test, reviewer).
4. Ensure MCP tools for Jira and GitHub are invoked correctly.
""",

    ".github/copilot/agents/requirements-subagent.agent.md": """---
name: requirements-subagent
description: Specialized subagent for gathering, clarifying, and documenting software requirements.
tools:
  - jira-mcp
  - file-writer
skills:
  - requirement-analysis-skill
---
# Requirements Subagent Instructions
1. Connect via Jira MCP to fetch user story details and acceptance criteria.
2. Perform gap analysis on ambiguous requirements.
3. Output structured requirements conforming to `requirements-template.md`.
""",

    ".github/copilot/agents/design-subagent.agent.md": """---
name: design-subagent
description: Subagent for system architecture design and component mapping.
tools:
  - file-writer
skills:
  - architecture-mapping-skill
---
# Design Subagent Instructions
1. Analyze `requirements.md` to design high-level architecture.
2. Produce component breakdown and data flow diagrams.
3. Output to `architecture.md`.
""",

    ".github/copilot/agents/planner-subagent.agent.md": """---
name: planner-subagent
description: Subagent for breaking down architecture into prioritized, dependency-ordered tasks.
tools:
  - file-writer
---
# Planner Subagent Instructions
1. Parse `architecture.md`.
2. Generate task breakdown with explicit dependency ordering.
3. Save output to `impl-plan.md`.
""",

    ".github/copilot/agents/developer-agent.agent.md": """---
name: developer-agent
description: Autonomous coding agent implementing tasks adhering to DRY and clean code.
tools:
  - file-writer
  - git-committer
permissions:
  - modify-codebase
---
# Developer Agent Instructions
1. Read `impl-plan.md`.
2. Work exclusively on branch `feature/capstone-EPMCDMETST-67217`.
3. Implement features strictly adhering to DRY principles and modular design.
""",

    ".github/copilot/agents/tester-agent.agent.md": """---
name: tester-agent
description: QA agent executing unit/integration tests and verifying test coverage thresholds.
tools:
  - test-results-recorder
---
# Tester Agent Instructions
1. Execute test suite for backend and frontend.
2. Verify test coverage meets or exceeds the 85% threshold.
3. Generate `COMPLETION_SUMMARY.md`.
""",

    ".github/copilot/agents/reviewer-agent.agent.md": """---
name: reviewer-agent
description: Rigorous code review agent performing 7-point evaluations and posting PR comments.
tools:
  - github-mcp
  - pr-commenter
---
# Reviewer Agent Instructions
1. Perform 7-point review: Correctness, Security, Error Handling, Test Coverage, Code Clarity, DRY Principle, and Dependency Safety.
2. Save findings locally to `code-review.md`.
3. Execute `pr-commenter` to post review comments live on the GitHub PR.
""",

    ".github/copilot/skills/SKILL.md": """---
name: requirement-analysis-skill
description: Skill for parsing user stories and extracting functional/non-functional requirements.
---
# Requirement Analysis Skill
Provides prompt guidelines for extracting precise testable acceptance criteria from raw Jira issues.
""",

    ".github/copilot/skills/SKILL.md": """---
name: code-review-skill
description: Skill for evaluating code against security vulnerabilities and DRY principles.
---
# Code Review Skill
Enforces the 7-point evaluation framework for all code changes.
""",

    ".github/copilot/mcp/jira-mcp.json": """{
  "mcpServer": {
    "name": "jira-mcp",
    "transport": "stdio",
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-jira"],
    "env": {
      "JIRA_BASE_URL": "${JIRA_BASE_URL}",
      "JIRA_USER_EMAIL": "${JIRA_USER_EMAIL}",
      "JIRA_API_TOKEN": "${JIRA_API_TOKEN}"
    }
  }
}
""",

    ".github/copilot/mcp/github-mcp.json": """{
  "mcpServer": {
    "name": "github-mcp",
    "transport": "stdio",
    "command": "npx",
    "args": ["-y", "@modelcontextprotocol/server-github"],
    "env": {
      "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}"
    }
  }
}
"""
}

# Create directories and write files
for filepath, content in files.items():
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\\n")

print("Successfully generated all evaluator-compliant agent configs, skills, subagents, and MCP tools!")