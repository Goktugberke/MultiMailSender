import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CompanyListItem {
  id?: number;
  city?: string;
  companyName: string;
  companyMail?: string;
  companyWebsite?: string;
  companyNumber?: string;
  notes?: string;
  addedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private readonly base = 'http://localhost:8080/companies';

  constructor(private http: HttpClient) {}

  getAll(city?: string, search?: string): Observable<CompanyListItem[]> {
    let params = new HttpParams();
    if (city)   params = params.set('city', city);
    if (search) params = params.set('search', search);
    return this.http.get<CompanyListItem[]>(this.base, { params });
  }

  getCities(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/cities`);
  }

  create(item: CompanyListItem): Observable<CompanyListItem> {
    return this.http.post<CompanyListItem>(this.base, item);
  }

  update(id: number, patch: Partial<CompanyListItem>): Observable<CompanyListItem> {
    return this.http.put<CompanyListItem>(`${this.base}/${id}`, patch);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  bulkImport(city: string, companies: Partial<CompanyListItem>[]): Observable<any> {
    return this.http.post<any>(`${this.base}/import`, { city, companies });
  }
}
