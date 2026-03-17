import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type ApplicationStatus =
  | 'SENT' | 'BOUNCED' | 'REPLIED' | 'EXAM' | 'INTERVIEW' | 'REJECTED' | 'ACCEPTED' | 'WITHDRAWN';

export interface ApplicationRecord {
  id?: number;
  companyName: string;
  companyMail: string;
  positionTitle: string;
  sentAt?: string;
  status: ApplicationStatus;
  notes?: string;
  examDate?: string;
  interviewDate?: string;
  followUpDate?: string;
}

export interface ApplicationStats {
  TOTAL?: number;
  SENT?: number;
  BOUNCED?: number;
  REPLIED?: number;
  EXAM?: number;
  INTERVIEW?: number;
  REJECTED?: number;
  ACCEPTED?: number;
  WITHDRAWN?: number;
  UPCOMING_FOLLOW_UPS?: number;
  UPCOMING_EXAMS?: number;
  [key: string]: number | undefined;
}

@Injectable({ providedIn: 'root' })
export class ApplicationService {
  private readonly base = 'http://localhost:8080/applications';

  constructor(private http: HttpClient) {}

  getAll(status?: string): Observable<ApplicationRecord[]> {
    const params = status ? `?status=${status}` : '';
    return this.http.get<ApplicationRecord[]>(`${this.base}${params}`);
  }

  getStats(): Observable<ApplicationStats> {
    return this.http.get<ApplicationStats>(`${this.base}/stats`);
  }

  update(id: number, patch: Partial<ApplicationRecord>): Observable<ApplicationRecord> {
    return this.http.put<ApplicationRecord>(`${this.base}/${id}`, patch);
  }

  delete(id: number): Observable<any> {
    return this.http.delete(`${this.base}/${id}`);
  }

  create(record: Partial<ApplicationRecord>): Observable<ApplicationRecord> {
    return this.http.post<ApplicationRecord>(this.base, record);
  }
}
