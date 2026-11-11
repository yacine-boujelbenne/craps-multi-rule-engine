# Maestro runbook

Maestro (RunMaestro/Maestro, AGPL-3.0) is a desktop app that drives agent CLIs you have already installed and logged in to. It adds parallel agents, git worktrees, Auto Run playbooks, Group Chat and a CLI.

## 1. Install (once)
1. Install and authenticate the agents you use: OpenAI Codex CLI and OpenCode (each with its own login/API key).
2. Install Maestro from https://github.com/RunMaestro/Maestro/releases, or build from source:
   `git clone https://github.com/RunMaestro/Maestro.git && cd Maestro && npm install && npm run dev`
3. Install Git, JDK 21, Node LTS, Docker.

## 2. Open this project
1. `git init && git add . && git commit -m "chore: initial scaffold"` in this folder.
2. In Maestro create the agents from `docs/02-agent-roster.md` (Cmd/Ctrl+N).
3. Open the Auto Run tab (Cmd/Ctrl+Shift+1) and select the folder `.maestro/playbooks`.
4. Each subfolder (Phase-0 ... Phase-4) holds numbered markdown checklists. Maestro runs open `- [ ]` tasks one by one, each in a fresh session.

## 3. Daily loop
1. `architect` plans or refines the next doc (spec first, then checklist).
2. Create worktree sub-agents from the git branch menu for parallel areas (backend + frontend at once).
3. Select the phase's documents in Auto Run, drag them into order, click Run. Use loop settings only for docs meant to repeat.
4. Review the History tab and the git diff. Merge branches one at a time; `qa` runs the full suite after each merge.
5. Use Group Chat with `architect` as moderator when the backend and frontend disagree about an API shape.

## 4. Headless / CI
`maestro-cli` can list agents and run playbooks from cron or CI with JSON output. See https://docs.runmaestro.ai/cli.

## 5. Safety
- Agents run with the permissions you set in Codex/OpenCode. Keep production credentials out of the repo and out of agent environments.
- Never put real customer documents in the repo; use anonymized samples in `samples/` (git-ignored if sensitive).

## Parallel-agent release gate
See `docs/MASTER-PLAN.md` §§14–16. Start with 1 architect + 2 disjoint implementers, use Git worktrees, serialize migrations/OpenAPI contract, require CI and human merge review. Never mark blocked tasks completed. Lucent UI styling is Angular-native and should be implemented in the frontend worktree only.
