# Gitnuro - Multiplatform Git Client
[![Latest release](https://img.shields.io/github/v/release/JetpackDuba/Gitnuro?color=blue&label=latest%20release)](https://github.com/JetpackDuba/Gitnuro/releases/latest)

![Icon](res/img/cover.png)

A FOSS Git client based on (Jetbrains) Compose and JGit.

The main goal of Gitnuro is to provide a multiplatform open source Git client without any kind of constraint to how you can use it nor relying on web technologies.

## Features

Gitnuro has support for the following features:

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
- Theming.
- Side by side diff in text files (will be available at 1.2.0).

As it's still a young project, there are some interesting features that are not yet implemented but will be in the future:

- Create/Apply patches
- Remove tags from remote.
- View stashes in the log tree.
- Submodules support (planned for 1.1.0).
- Change the tracking of a specific branch.

## Contributing

If you find a bug or you would like to suggest a new feature, feel free to open an issue.

Pull requests are also welcome but please create an issue first if it's a new feature.

## FAQ

> Is Gitnuro completly free?

Yes, free in both meanings of the word (in money and freedom).

> Does Gitnuro keep track of my data?

Gitnuro does not track data in any way, don't worry.

> I don't like the built-in themes, can I create a custom one?

Gitnuro includes the option to set custom themes in a JSON format. Keep in mind that themes may break with new releases, making the default theme the fallback option.

For the latest stable version (1.0.1), you can use this JSON as an example:

```
{
    "primary": "FF456b00",
    "primaryVariant": "FF456b00",
    "onPrimary": "FFFFFFFFF",
    "secondary": "FF9c27b0",
    "primaryText": "FF141f00",
    "secondaryText": "FF595858",
    "error": "FFc93838",
    "onError": "FFFFFFFF",
    "background": "FFe7f2d3",
    "backgroundSelected": "C0cee1f2",
    "surface": "FFc5f078",
    "secondarySurface": "FFedeef2",
    "headerBackground": "FFF4F6FA",
    "borderColor": "FF989898",
    "graphHeaderBackground": "FFF4F6FA",
    "addFile": "FF32A852",
    "deletedFile": "FFc93838",
    "modifiedFile": "FF0070D8",
    "conflictingFile": "FFFFB638",
    "dialogOverlay": "AA000000",
    "normalScrollbar": "FFCCCCCC",
    "hoverScrollbar": "FF0070D8"
}
```

Colors are in ARGB Hex format.

> Why isn't the Mac version signed? 

The cost of the Apple Developer Program is quite high with a platform that currently has very few users. I may pay for it if it's a very requested feature but not for now.

> Authentication has failed. What's wrong?

Currently there are some limitations regarding this topic. Here are some known problematic setups:
- SSH keys managed by external agents (https://github.com/JetpackDuba/Gitnuro/issues/17).
- Configurations added to .ssh/config
- Non-default ssh dir path.
- Multicast DNS remote URL (https://github.com/JetpackDuba/Gitnuro/issues/19).

If the authentication fails and you think its due to a different reason, please open a new issue.
