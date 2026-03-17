import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ApplicationService, ApplicationRecord, ApplicationStats, ApplicationStatus } from '../../service/application.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class DashboardComponent implements OnInit {
  applications: ApplicationRecord[] = [];
  filtered: ApplicationRecord[] = [];
  stats: ApplicationStats = {};
  activeFilter = '';
  editingId: number | null = null;
  editPatch: Partial<ApplicationRecord> = {};
  loading = false;
  error = '';
  bounceChecking = false;
  bounceMessage = '';
  // Bounce kontrolü için credentials
  bounceEmail = '';
  bouncePassword = '';
  showBouncePanel = false;

  readonly statuses: ApplicationStatus[] = [
    'SENT', 'BOUNCED', 'REPLIED', 'EXAM', 'INTERVIEW', 'REJECTED', 'ACCEPTED', 'WITHDRAWN'
  ];

  readonly statusLabels: Record<ApplicationStatus, string> = {
    SENT: 'Gönderildi', BOUNCED: 'Geri Döndü', REPLIED: 'Cevap Geldi',
    EXAM: 'Sınav', INTERVIEW: 'Mülakat', REJECTED: 'Reddedildi',
    ACCEPTED: 'Kabul', WITHDRAWN: 'İptal'
  };

  readonly statCards = [
    { key: 'TOTAL',     label: 'Toplam',       icon: 'bi-send-fill',         cls: 'sc-blue'   },
    { key: 'EXAM',      label: 'Sınav',         icon: 'bi-pencil-square',     cls: 'sc-amber'  },
    { key: 'INTERVIEW', label: 'Mülakat',       icon: 'bi-person-video3',     cls: 'sc-cyan'   },
    { key: 'ACCEPTED',  label: 'Kabul',         icon: 'bi-trophy-fill',       cls: 'sc-green'  },
    { key: 'REPLIED',   label: 'Cevap Geldi',   icon: 'bi-reply-all-fill',    cls: 'sc-purple' },
    { key: 'SENT',      label: 'Beklemede',     icon: 'bi-clock-fill',        cls: 'sc-slate'  },
    { key: 'REJECTED',  label: 'Reddedildi',    icon: 'bi-x-circle-fill',     cls: 'sc-red'    },
    { key: 'BOUNCED',   label: 'Geri Döndü',    icon: 'bi-exclamation-triangle-fill', cls: 'sc-orange' },
  ];

  constructor(private appService: ApplicationService, private http: HttpClient) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.appService.getAll().subscribe({
      next: apps => {
        this.applications = apps.sort((a, b) =>
          new Date(b.sentAt || 0).getTime() - new Date(a.sentAt || 0).getTime()
        );
        this.applyFilter();
        this.loading = false;
      },
      error: () => { this.error = 'Veriler yüklenemedi.'; this.loading = false; }
    });
    this.appService.getStats().subscribe({
      next: s => this.stats = s,
      error: () => {}
    });
  }

  setFilter(status: string) {
    this.activeFilter = this.activeFilter === status ? '' : status;
    this.applyFilter();
  }

  applyFilter() {
    this.filtered = this.activeFilter
      ? this.applications.filter(a => a.status === this.activeFilter)
      : this.applications;
  }

  statusOf(count: number | undefined): number { return count ?? 0; }

  startEdit(app: ApplicationRecord) {
    this.editingId = app.id!;
    this.editPatch = {
      status: app.status,
      notes: app.notes || '',
      positionTitle: app.positionTitle,
      examDate:      app.examDate      ? app.examDate.substring(0, 16)      : '',
      interviewDate: app.interviewDate ? app.interviewDate.substring(0, 16) : '',
      followUpDate:  app.followUpDate  ? app.followUpDate.substring(0, 16)  : ''
    };
  }

  saveEdit(id: number) {
    const patch: any = { ...this.editPatch };
    if (!patch.examDate)      patch.examDate      = null;
    if (!patch.interviewDate) patch.interviewDate = null;
    if (!patch.followUpDate)  patch.followUpDate  = null;
    this.appService.update(id, patch).subscribe({
      next: () => { this.editingId = null; this.load(); },
      error: () => { this.error = 'Güncelleme başarısız.'; }
    });
  }

  cancelEdit() { this.editingId = null; }

  delete(id: number) {
    if (!confirm('Bu başvuruyu silmek istediğinize emin misiniz?')) return;
    this.appService.delete(id).subscribe({ next: () => this.load() });
  }

  hasUpcomingAlert(): boolean {
    return (this.stats['UPCOMING_FOLLOW_UPS'] ?? 0) > 0 || (this.stats['UPCOMING_EXAMS'] ?? 0) > 0;
  }

  trackById(_: number, app: ApplicationRecord) { return app.id; }

  daysSince(sentAt: string | undefined): number {
    if (!sentAt) return 0;
    const diff = Date.now() - new Date(sentAt).getTime();
    return Math.floor(diff / (1000 * 60 * 60 * 24));
  }

  checkBounces() {
    if (!this.bounceEmail.trim() || !this.bouncePassword.trim()) return;
    this.bounceChecking = true;
    this.bounceMessage = '';
    this.http.post<any>('http://localhost:8080/imap/check-bounces', {
      email: this.bounceEmail.trim(),
      appPassword: this.bouncePassword.trim()
    }).subscribe({
      next: res => {
        this.bounceMessage = res.message || 'Kontrol tamamlandı';
        this.bounceChecking = false;
        this.showBouncePanel = false;
        this.load();
      },
      error: err => {
        this.bounceMessage = err?.error?.message || 'Bağlantı hatası';
        this.bounceChecking = false;
      }
    });
  }
}
