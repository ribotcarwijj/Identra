## Identra 0.7.39

- **Native stealth for every app**: turning on the *Hide environment* group for an app now also hides root and the hook framework from the app's native code, not only from Java. Covered: `/proc/self/maps`, `smaps`, `mountinfo`, loaded-library lists (`dl_iterate_phdr`), su / Magisk / LSPosed / KernelSU files (open, access, stat) and directory listings.
- Works for any app, no per-app setup. Apps that make raw system calls themselves can still see more; that needs a deeper layer (planned).
- Includes 0.7.38: *All groups* switch (on and off), long-press a group to pick single hooks.

