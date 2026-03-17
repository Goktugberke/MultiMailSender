import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CvService, CvProfile } from '../../service/cv.service';

@Component({
  selector: 'app-cv',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cv.html',
  styleUrls: ['./cv.css']
})
export class CvComponent implements OnInit {
  profile: CvProfile | null = null;
  loading = false;
  uploading = false;
  reanalyzing = false;
  aiConfigured: boolean | null = null;
  error = '';
  success = '';

  // Mail üretici
  genCompany = '';
  genPosition = 'Staj/İş Başvurusu';
  genWebsite = '';
  generatedMail = '';
  generating = false;

  // Analiz JSON'u parse edilmiş hali
  parsedAnalysis: any = null;

  constructor(private cvService: CvService) {}

  ngOnInit() {
    this.loadProfile();
    this.cvService.getAiStatus().subscribe({
      next: res => this.aiConfigured = res.data === true,
      error: () => this.aiConfigured = false
    });
  }

  loadProfile() {
    this.loading = true;
    this.cvService.getProfile().subscribe({
      next: res => {
        if (res && res.id) {
          this.profile = res as CvProfile;
          this.parseAnalysis();
        }
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      this.error = 'Lütfen PDF formatında bir CV yükleyin.';
      return;
    }
    this.uploading = true;
    this.error = '';
    this.success = '';
    this.cvService.upload(file).subscribe({
      next: profile => {
        this.profile = profile;
        this.parseAnalysis();
        this.success = 'CV yüklendi ve analiz edildi!';
        this.uploading = false;
      },
      error: err => {
        this.error = 'Yükleme başarısız: ' + (err?.error?.message || err?.message || 'Bilinmeyen hata');
        this.uploading = false;
      }
    });
    input.value = '';
  }

  reanalyze() {
    if (!this.profile) return;
    this.reanalyzing = true;
    this.error = '';
    this.cvService.reanalyze(this.profile.id).subscribe({
      next: profile => {
        this.profile = profile;
        this.parseAnalysis();
        this.success = 'CV yeniden analiz edildi!';
        this.reanalyzing = false;
      },
      error: () => { this.error = 'Yeniden analiz başarısız.'; this.reanalyzing = false; }
    });
  }

  generateMail() {
    if (!this.genCompany.trim()) { this.error = 'Şirket adı girin.'; return; }
    this.generating = true;
    this.error = '';
    const obs = this.genWebsite.trim()
      ? this.cvService.generateMailWithWeb(this.genCompany, this.genWebsite.trim(), this.genPosition)
      : this.cvService.generateMail(this.genCompany, this.genPosition);
    obs.subscribe({
      next: res => { this.generatedMail = res.data || ''; this.generating = false; },
      error: err => {
        this.error = err?.error?.message || 'Mail üretimi başarısız.';
        this.generating = false;
      }
    });
  }

  copyMail() {
    navigator.clipboard.writeText(this.generatedMail).then(() => {
      this.success = 'Mail kopyalandı!';
      setTimeout(() => this.success = '', 2000);
    });
  }

  private parseAnalysis() {
    if (!this.profile?.analysisJson) return;
    try {
      let json = this.profile.analysisJson.trim();
      // Gemini bazen ```json ... ``` ile sarar
      if (json.startsWith('```')) {
        json = json.replace(/^```json?\n?/, '').replace(/\n?```$/, '');
      }
      this.parsedAnalysis = JSON.parse(json);
    } catch {
      this.parsedAnalysis = null;
    }
  }
}
