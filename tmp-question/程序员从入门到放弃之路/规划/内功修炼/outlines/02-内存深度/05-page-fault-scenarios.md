# 缺页异常四场景 — #PF 来了之后内核做什么

> Cluster B: 6 KPs | 依赖: 04-mmstruct-vma | 读者基线: mm_struct + VMA 建立流程

---

### 1. 匿名缺页 do_anonymous_page — 首次访问时的零页分配
  - 触发: 访问 mmap MAP_ANONYMOUS|MAP_PRIVATE 地址 → MMU → PTE Present=0 → #PF → `do_user_addr_fault → handle_mm_fault → handle_pte_fault` (arch/x86/mm/fault.c → do_user_addr_fault)
  - 只读访问: `do_anonymous_page → pte_alloc` 分配 PTE → 映射 zero page(`my_zero_pfn` 全零共享页, `_PAGE_SPECIAL|_PAGE_PFN=zero_pfn`, 只读) (mm/memory.c:4096)
  - 写访问: `alloc_zeroed_user_highpage_movable(gfp)` 通过 Buddy 分配零页 → `pte_mkwrite` 可写 → `__SetPageUptodate` (mm/memory.c:4131)
  - `page_add_new_anon_rmap` 建立匿名反向映射 → `lru_cache_add` 加入 LRU → `set_pte_at` 设置 PTE (mm/rmap.c → page_add_new_anon_rmap, mm/swap.c → lru_cache_add)
  - 首次缺页延迟: ~1-2 微秒(Buddy 分配 + zeroing + PTE 设置)

### 2. COW 缺页 do_wp_page — fork 后的写时复制
  - fork → 子进程拷贝父进程页表 → 所有可写页标记 `_PAGE_RW=0`(只读) → 子进程写 → #PF(write=1, present=1) (kernel/fork.c)
  - `handle_pte_fault → do_wp_page`: 检查 `page_count(page) > 1`(多次共享) → 需要 COW (mm/memory.c:3291)
  - `wp_page_copy`: `alloc_page_vma(GFP_HIGHUSER_MOVABLE, vma, addr)` 分配新物理页 → `cow_user_page(kmap_atomic 拷贝)` (mm/memory.c:2876)
  - `page_remove_rmap(old_page)` 减旧页映射计数 → `page_add_new_anon_rmap(new_page)` → `lru_cache_add` (mm/memory.c → page_remove_rmap)
  - `ptep_clear_flush` 清除旧 PTE+TLB flush → `set_pte_at_notify` 设置新 PTE 可写+新页 → 父/子各自独立物理页
  - 优化: `reuse_old_page`: `page_count=1`(仅当前进程映射) → 直接 `pte_mkwrite` 跳过拷贝

### 3. 文件缺页 do_fault — File-Backed 映射的三条分支
  - 访问 mmap 文件地址 → #PF → `handle_pte_fault → do_fault` (mm/memory.c:4460)
  - **MAP_SHARED 写**: `do_shared_fault → __do_fault → vma→vm_ops→fault → filemap_fault → do_sync_mmap_readahead` 预读 → `__filemap_get_folio` (mm/memory.c:4490)
  - 页缓存未命中 → `readpage → submit_bio → IO` → `finish_fault → alloc_page → page_add_file_rmap → set_pte_at` (mm/filemap.c:3200)
  - **MAP_PRIVATE 写**: `do_cow_fault → __do_fault` 读原页 → `do_wp_page` COW 分支 → 写不传播到文件 (mm/memory.c:4520)
  - **MAP_PRIVATE 读**: `do_read_fault → __do_fault → finish_fault` → 映射页缓存页(只读) (mm/memory.c:4540)

### 4. 栈扩展与 GUP — 原子访问与栈自动增长
  - 栈: 访问栈下方未映射区域 → #PF → `handle_mm_fault → do_anonymous_page → expand_stack(vma, addr)` (mm/memory.c → handle_mm_fault)
  - `expand_stack`: 检查 `vma→vm_start - stack_guard_gap - PAGE_SIZE` + RLIMIT_STACK → `vma→vm_start -= PAGE_SIZE × grow` (mm/mmap.c → expand_stack)
  - 保护页: guard page 导致 #PF → SIGSEGV 而非扩展 → `pthread_attr_setguardsize` (mm/mmap.c → guard page)
  - **GUP**(Get User Pages): 内核访问用户地址(O_DIRECT/splice) → `get_user_pages → __get_user_pages → find_vma → follow_page_mask → try_grab_folio` (mm/gup.c)
  - GUP flags: `FOLL_WRITE/FOLL_FORCE/FOLL_PIN/FOLL_LONGTERM` — pin 过多引发内存压力, FOLL_LONGTERM 与 MOVABLE 冲突 → CMA 区域 preferred

### 5. 收束
  - 四种 #PF 场景分治: 匿名缺页(零页分配)→COW(fork后写时复制)→文件缺页(预读+页缓存)→栈扩展(VMA自动增长)
  - COW 优化: `page_count=1` 时 `reuse_old_page` 直接改 PTE 可写跳过拷贝, 否则 `wp_page_copy` 分配新页
  - 文件缺页三条分支: MAP_SHARED 写→页缓存命中的快速路径, MAP_PRIVATE 写→COW 隔离, MAP_PRIVATE 读→只读映射

---

### 核心悬念
**"文件页缺页时数据从磁盘读到页缓存 — address_space 的 xarray 怎么组织？预读算法怎么猜中下一次访问？"**

→ 引出 06 页缓存 — xarray + Readahead + Fault-Around + Direct I/O
