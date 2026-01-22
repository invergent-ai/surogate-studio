import {Component, computed, EventEmitter, input, Output, signal, viewChild} from '@angular/core';
import {Copy, Download, File, Folder, LucideAngularModule, Trash} from 'lucide-angular';
import {NgxFilesizeModule} from 'ngx-filesize';
import {PrimeTemplate, TreeNode} from 'primeng/api';
import {TreeTableModule} from 'primeng/treetable';
import dayjs from 'dayjs/esm';
import {ILakeFsObjectStats, ILakeFsRepository} from '../../../shared/model/lakefs.model';
import {Button, ButtonDirective} from 'primeng/button';
import {OverlayPanel, OverlayPanelModule} from 'primeng/overlaypanel';
import {MenuModule} from 'primeng/menu';
import {NgIf} from "@angular/common";

export interface FileActionEvent {
  action: 'download' | 'copy' | 'delete';
  file: ILakeFsObjectStats;
}

@Component({
  selector: 'sm-file-navigator',
  standalone: true,
  templateUrl: './file-navigator.component.html',
  imports: [
    LucideAngularModule,
    NgxFilesizeModule,
    PrimeTemplate,
    TreeTableModule,
    Button,
    OverlayPanelModule,
    MenuModule,
    NgIf,
    ButtonDirective
  ]
})
export class FileNavigatorComponent {
  objects = input.required<ILakeFsObjectStats[]>();
  selectedPath= input<TreeNode>();
  repository = input.required<ILakeFsRepository>();
  // 'file' | 'folder' | 'any' - currently not implemented
  type = input<'file' | 'folder' | 'any'>('any');
  // whether to show action menu on each item
  showItemActions = input(true);
  // if folders / files are selectable (used as a selection componen
  selectable = input(false);

  fileMenu = viewChild(OverlayPanel);

  @Output()
  onSelectFile = new EventEmitter<TreeNode<ILakeFsObjectStats>>();
  @Output()
  onSelectFolder = new EventEmitter<TreeNode>();
  @Output()
  onFileAction = new EventEmitter<FileActionEvent>()

  // Build a hierarchical representation of objects similar to a filesystem tree.
  allNodes = computed(() => {
    const lakeObjects = this.objects() ?? [];
    if (!lakeObjects || lakeObjects.length === 0) {
      return [] as TreeNode<ILakeFsObjectStats | DirData>[];
    }
    return buildTreeNodes(lakeObjects);
  });

  displayedNodes = computed(() => this.listFolderContents(this.selectedFolder() || this.selectedPath()));
  // Allow null explicitly for initial state
  selectedFolder = signal<TreeNode<DirData> | null>(null);

  // Map of key -> original tree node (with intact parent links)
  nodeIndex = computed(() => {
    const map = new Map<string, TreeNode<ILakeFsObjectStats | DirData>>();
    const roots = this.allNodes();
    const stack = [...roots];
    while (stack.length) {
      const n = stack.pop();
      if (!n) continue;
      map.set(n.key, n);
      if (n.children?.length) stack.push(...n.children);
    }
    return map;
  });

  actionFile = signal<ILakeFsObjectStats>(null);

  showItemMenu(event: MouseEvent, file: ILakeFsObjectStats): void {
    this.fileMenu().show(event);
    this.actionFile.set(file);
  }

  triggerFileAction(action: string) {
    this.onFileAction.emit({action, file: this.actionFile()} as FileActionEvent);
    this.fileMenu().hide();
  }

  /**
   * Returns the contents of a folder, plus a leading ".." entry if it has a parent.
   * Does NOT mutate the original tree nodes (creates shallow display clones instead).
   *
   * @param selectedFolder Directory node from buildTreeNodes
   */
  listFolderContents(selectedFolder: TreeNode<DirData> | null): TreeNode<ILakeFsObjectStats | DirData | UpData>[] {
    // If no folder is selected, show the root nodes (already sorted + indexed by buildTreeNodes).
    if (!selectedFolder) {
      return this.allNodes();
    }

    const originalChildren = selectedFolder.children ?? [];

    // Sort copy of children alphabetically, directories first.
    const sortedChildren = [...originalChildren].sort((a, b) => {
      const aIsDir = a.type === 'dir';
      const bIsDir = b.type === 'dir';
      if (aIsDir !== bIsDir) return aIsDir ? -1 : 1;
      return (a.label ?? '').localeCompare(b.label ?? '', undefined, {
        sensitivity: 'base',
        numeric: true,
      });
    });

    const displayItems: TreeNode<ILakeFsObjectStats | DirData | UpData>[] = [];

    // Always provide an up navigation entry for any selected folder.
    const parent = selectedFolder.parent as TreeNode<ILakeFsObjectStats | DirData> | undefined;
    const parentData = parent?.data as DirData | undefined;
    displayItems.push({
      key: `${selectedFolder.key}__UP__`,
      label: '..',
      type: 'up',
      leaf: true,
      data: {
        kind: 'up',
        targetKey: parent?.key,          // undefined when at root-level folder -> will navigate to root
        targetFullPath: parentData?.fullPath,
      },
    });

    // Shallow clone nodes so we don't mutate original tree with index/lastChild used for rendering.
    for (const child of sortedChildren) {
      displayItems.push({ ...child });
    }

    // Assign index + lastChild metadata for UI rendering consistency (on display clones only).
    displayItems.forEach((n, i) => {
      (n as any).index = i; // intentional: index/lastChild are view concerns, not in TreeNode interface
      (n as any).lastChild = i === displayItems.length - 1;
    });

    return displayItems;
  }

  /**
   * Select a folder (or navigate up) based on a clicked node from the UI.
   * Ensures we store the ORIGINAL node instance (with parent links) in selectedFolder.
   */
  selectFolder(node: TreeNode) {
    if (!node) {
      this.selectedFolder.set(null);
      return;
    }
    if (node.type === 'up') {
      const key = (node.data as UpData)?.targetKey;
      if (!key) {
        this.selectedFolder.set(null);
        return;
      }
      const parentNode = this.nodeIndex().get(key);
      this.selectedFolder.set(parentNode as TreeNode<DirData> || null);
      return;
    }
    if (node.type === 'dir') {
      const original = this.nodeIndex().get(node.key);
      this.selectedFolder.set(original as TreeNode<DirData> || null);
      return;
    }
    // Ignore clicks on files for folder selection.
  }

  selectFolder2(node: TreeNode) {
    if (!node) {
      return;
    }

    if (node.type === 'dir') {
      const original = this.nodeIndex().get(node.key);
      this.selectedFolder.set(original as TreeNode<DirData> || null);
      this.onSelectFolder.emit(original as TreeNode<DirData> || null);
    }
  }

  selectFile(node: TreeNode) {
    // Get the original node from our map to ensure all links (like parent) are correct.
    const originalNode = this.nodeIndex().get(node.key) as TreeNode<ILakeFsObjectStats> | undefined;

    if (originalNode) {
      this.onSelectFile.emit(originalNode);
    }
  }

  trimFilename(text: string): string {
    const MAX_LENGTH = 50;
    if (!text) {
      return '';
    }

    if (text.length > MAX_LENGTH) {
      return `${text.slice(0, MAX_LENGTH-3)}...`;
    } else {
      return text;
    }
  }

  protected readonly Folder = Folder;
  protected readonly File = File;
  protected readonly dayjs = dayjs;
  protected readonly Trash = Trash;
  protected readonly Download = Download;
  protected readonly Copy = Copy;
}

export interface DirData {
  kind: 'dir';
  fullPath: string; // full path of the folder (no trailing slash)
}

export interface UpData {
  kind: 'up';
  targetKey?: string;        // key of the folder to navigate to
  targetFullPath?: string;   // full path of the target folder
}

export function buildTreeNodes(
  objects: ILakeFsObjectStats[]
): TreeNode<ILakeFsObjectStats | DirData>[] {
  const roots: TreeNode<ILakeFsObjectStats | DirData>[] = [];
  // A map to quickly find any created directory node to avoid duplicates.
  const byKey = new Map<string, TreeNode<ILakeFsObjectStats | DirData>>();

  const ensureDir = (
    fullDirPath: string,
    label: string,
    parent: TreeNode<ILakeFsObjectStats | DirData> | undefined
  ): TreeNode<ILakeFsObjectStats | DirData> => {
    // Standardize directory keys to always end with a slash.
    const key = fullDirPath.endsWith('/') ? fullDirPath : `${fullDirPath}/`;
    let node = byKey.get(key);
    if (!node) {
      node = {
        key,
        label,
        type: 'dir',
        leaf: false,
        data: { kind: 'dir', fullPath: fullDirPath },
        children: [],
        parent,
      };
      byKey.set(key, node);
      if (parent) {
        parent.children?.push(node);
      } else {
        roots.push(node);
      }
    }
    return node;
  };

  for (const obj of objects) {
    // Trim leading/trailing slashes for consistent path splitting.
    const rawPath = obj.path.replace(/^\/+|\/+$/g, '');
    if (!rawPath) continue;

    const parts = rawPath.split('/');
    const fileName = parts[parts.length - 1];

    // This variable will hold the immediate parent of the current object.
    // It starts as 'undefined' for root-level objects.
    let parent: TreeNode<ILakeFsObjectStats | DirData> | undefined = undefined;
    let accumulatedPath = '';

    // This loop creates the directory structure for the object.
    // It intentionally stops before the last part (the filename).
    for (let i = 0; i < parts.length - 1; i++) {
      accumulatedPath = accumulatedPath ? `${accumulatedPath}/${parts[i]}` : parts[i];
      parent = ensureDir(accumulatedPath, parts[i], parent);
    }

    // Create the file node.
    // **If the loop above did not run (for a root file), 'parent' is still undefined. This is correct.**
    const fileNode: TreeNode<ILakeFsObjectStats | DirData> = {
      key: rawPath,
      label: fileName,
      type: 'file',
      leaf: true,
      data: obj,
      parent, // Assign the determined parent (or undefined if it's a root file).
    };

    // Add the file node to its parent's children array or to the roots array.
    if (parent) {
      parent.children?.push(fileNode);
    } else {
      roots.push(fileNode);
    }
  }

  // Recursive alphabetical sort (this part is unchanged and works well).
  const sortChildren = (nodes: TreeNode[]): void => {
    nodes.sort((a, b) => {
      const aIsDir = a.type === 'dir';
      const bIsDir = b.type === 'dir';
      if (aIsDir !== bIsDir) return aIsDir ? -1 : 1; // Dirs first.
      return (a.label ?? '').localeCompare(b.label ?? '', undefined, {
        sensitivity: 'base',
        numeric: true,
      });
    });
    for (const [idx, n] of nodes.entries()) {
      (n as any).index = idx;
      (n as any).lastChild = idx === nodes.length - 1;
      if (n.children?.length) sortChildren(n.children);
    }
  };
  sortChildren(roots);

  return roots;
}

