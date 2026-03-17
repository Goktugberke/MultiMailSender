import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface JobListing {
  title: string;
  company: string;
  location: string;
  source: string;
  url: string;
  postedAt?: string;
  description?: string;
  category?: string;
}

@Injectable({ providedIn: 'root' })
export class JobService {
  private readonly base = 'http://localhost:8080/jobs';

  constructor(private http: HttpClient) {}

  getJobs(q?: string, category?: string): Observable<JobListing[]> {
    let params = new HttpParams();
    if (q) params = params.set('q', q);
    if (category) params = params.set('category', category);
    return this.http.get<JobListing[]>(this.base, { params });
  }

  refresh(q?: string, category?: string): Observable<JobListing[]> {
    let params = new HttpParams();
    if (q) params = params.set('q', q);
    if (category) params = params.set('category', category);
    return this.http.post<JobListing[]>(`${this.base}/refresh`, null, { params });
  }
}
