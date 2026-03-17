import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CvProfile {
  id: number;
  fileName: string;
  rawText: string;
  analysisJson: string;
  uploadedAt: string;
  analysisUpdatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class CvService {
  private readonly base = 'http://localhost:8080/cv';

  constructor(private http: HttpClient) {}

  getProfile(): Observable<CvProfile | any> {
    return this.http.get<CvProfile>(`${this.base}/profile`);
  }

  upload(file: File): Observable<CvProfile> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<CvProfile>(`${this.base}/upload`, form);
  }

  reanalyze(id: number): Observable<CvProfile> {
    return this.http.post<CvProfile>(`${this.base}/reanalyze/${id}`, null);
  }

  generateMail(companyName: string, positionHint: string, cvAnalysis?: string): Observable<any> {
    return this.http.post<any>(`${this.base}/generate-mail`, { companyName, positionHint, cvAnalysis });
  }

  generateMailWithWeb(companyName: string, websiteUrl: string, positionHint: string, cvAnalysis?: string): Observable<any> {
    return this.http.post<any>(`${this.base}/generate-mail-with-web`, { companyName, websiteUrl, positionHint, cvAnalysis });
  }

  getAiStatus(): Observable<any> {
    return this.http.get<any>(`${this.base}/ai-status`);
  }
}
