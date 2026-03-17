import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { JobService, JobListing } from '../../service/job.service';

interface SourceConfig {
  id: string;
  label: string;
  tabCls: string;
  pillCls: string;
}

@Component({
  selector: 'app-jobs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './jobs.html',
  styleUrls: ['./jobs.css']
})
export class JobsComponent implements OnInit {
  jobs: JobListing[] = [];
  filtered: JobListing[] = [];
  query = '';
  loading = false;
  refreshing = false;
  error = '';
  activeSource = '';

  readonly sources: SourceConfig[] = [
    { id: '',              label: 'Tümü',          tabCls: 'all-tab',    pillCls: 'sp-default'  },
    { id: 'Rementist',    label: 'Rementist',     tabCls: 'st-rementist', pillCls: 'sp-rementist' },
    { id: 'YouthAll',     label: 'YouthAll',      tabCls: 'st-youthall',  pillCls: 'sp-youthall'  },
    { id: 'SavunmaKariyer', label: 'Savunma',     tabCls: 'st-savunma',   pillCls: 'sp-savunma'   },
    { id: 'KariyerNet',   label: 'KariyerNet',    tabCls: 'st-kariyer',   pillCls: 'sp-kariyer'   },
    { id: 'Indeed',       label: 'Indeed',         tabCls: 'st-indeed',    pillCls: 'sp-indeed'    },
    { id: 'SecretCV',     label: 'SecretCV',      tabCls: 'st-secret',    pillCls: 'sp-secret'    },
    { id: 'Yenibiris',    label: 'Yenibiris',     tabCls: 'st-yenibiris', pillCls: 'sp-yenibiris' },
    { id: 'Eleman.net',   label: 'Eleman.net',    tabCls: 'st-eleman',    pillCls: 'sp-eleman'    },
  ];

  constructor(private jobService: JobService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.error = '';
    this.jobService.getJobs(this.query || undefined).subscribe({
      next: jobs => { this.jobs = jobs; this.applyFilter(); this.loading = false; },
      error: () => { this.error = 'İlanlar yüklenemedi. Backend çalışıyor mu?'; this.loading = false; }
    });
  }

  refresh() {
    this.refreshing = true;
    this.error = '';
    this.jobService.refresh(this.query || undefined).subscribe({
      next: jobs => { this.jobs = jobs; this.applyFilter(); this.refreshing = false; },
      error: () => { this.error = 'Yenileme başarısız.'; this.refreshing = false; }
    });
  }

  setSource(id: string) {
    this.activeSource = id;
    this.applyFilter();
  }

  applyFilter() {
    let list = this.jobs;
    if (this.activeSource) list = list.filter(j => j.source === this.activeSource);
    if (this.query.trim()) {
      const q = this.query.toLowerCase();
      list = list.filter(j =>
        (j.title || '').toLowerCase().includes(q) ||
        (j.company || '').toLowerCase().includes(q)
      );
    }
    this.filtered = list;
  }

  countFor(sourceId: string): number {
    if (!sourceId) return this.jobs.length;
    return this.jobs.filter(j => j.source === sourceId).length;
  }

  pillFor(source: string): string {
    return this.sources.find(s => s.id === source)?.pillCls ?? 'sp-default';
  }

  avatarLetter(company: string): string {
    return (company || '?')[0].toUpperCase();
  }

  trackByIdx(i: number) { return i; }
}
