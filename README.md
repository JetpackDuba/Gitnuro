# Gitnuro

A FOSS Git client based on (Jetbrains) Compose and JGit.

The main goal of Gitnuro is to provide a multiplatform open source Git client without any kind of constraint to how you
can use it nor relying on web technologies.

The project it is still in alpha and many features are lacking or missing, but can be good for basic usage.

Right now you CAN:

- View diffs for text based files.
- View your history log and all its branches.
- Add (stage) & reset (unstage) files.
- Stage & unstage of hunks.
- Checkout files (revert changes of uncommited files).
- Clone.
- Commit.
- Reset commits.
- Revert commits.
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

Right now you CAN'T:

- Rebase interactive.
- Manage remotes.
- Remove tags from remote.
- Side by side diff in text files.
- Start a new local repository.
- View stashes in the log tree.
- View file history
- Blame file.
- Search by commit message/author/commit id.

## Releases

I'll create releases once we hit beta stage.

## Steps to build

Note: Requires minimum JDK 16.

- Clone the project
- Open terminal/shell in the project folder
- `./gradlew run` to run the project
- `./gradlew tasks` to view other build options (native building requires java >=15)

Feel free to open issues for bugs or sugestions.

## Screenshots (latest update: 01 feb 2022)

![Example 1](/res/img/gitnuro_example_1.png)
![Example 2](/res/img/gitnuro_example_2.png)
![Example 3](/res/img/gitnuro_example_3.png)
![Example 4](/res/img/gitnuro_example_4.png)
![Example 5](/res/img/gitnuro_example_5.png)
![Example 6](/res/img/gitnuro_example_6.png)
