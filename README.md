# Gitnuro

A FOSS Git client based on (Jetbrains) Compose and JGit.

The main goal of Gitnuro is to provide a multiplatform open source Git client without any kind of constraint to how you
can use it nor relying on web technologies.

The project it is still in early stages and many features are lacking or missing, but it's stable for daily usage.

Gitnuro features:

- View diffs for text based files.
- View your history log and all its branches.
- Add (stage) & reset (unstage) files.
- Stage & unstage of hunks.
- Checkout files (revert changes of uncommited files).
- Clone.
- Commit.
- Reset commits.
- Revert commits.
- Amend previous commit.
- Merge.
- Rebase.
- Create and delete branches locally.
- Create and delete tags locally.
- View remote branches.
- Pull and push.
- Stash and pop stash.
- Checkout a commit (detached HEAD).
- View changes/diff in images (side to side comparison).
- Force push.
- Remove branches from remote.
- Manage remotes.
- Start a new local repository.
- Search by commit message/author/commit id.
- Rebase interactive.
- Blame file.
- View file history.

Missing:

- Create/Apply patches
- Remove tags from remote.
- Side by side diff in text files.
- View stashes in the log tree.
- Submodules support.

## Steps to build

Note: Requires minimum JDK 16.

- Clone the project
- Open terminal/shell in the project folder
- `./gradlew run` to run the project
- `./gradlew tasks` to view other build options (native building requires java >=15)

Feel free to open issues for bugs or sugestions.

## Screenshots (latest update: 04 apr 2022)

![Example 1](/res/img/example_1.png)
![Example 2](/res/img/example_2.png)
![Example 3](/res/img/example_3.png)
![Example 4](/res/img/example_4.png)
