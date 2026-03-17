import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CompanyService, CompanyListItem } from '../../service/company.service';

@Component({
  selector: 'app-companies',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './companies.html',
  styleUrls: ['./companies.css']
})
export class CompaniesComponent implements OnInit {
  companies: CompanyListItem[] = [];
  cities: string[] = [];
  loading = false;
  error = '';
  success = '';

  // Filtreler
  filterCity = '';
  filterSearch = '';

  // Yeni şirket formu
  showAddForm = false;
  newItem: Partial<CompanyListItem> = { city: '', companyName: '', companyMail: '', companyWebsite: '', companyNumber: '' };

  // Düzenleme
  editingId: number | null = null;
  editItem: Partial<CompanyListItem> = {};

  constructor(private service: CompanyService) {}

  ngOnInit() {
    this.load();
    this.loadCities();
  }

  load() {
    this.loading = true;
    this.service.getAll(this.filterCity || undefined, this.filterSearch || undefined).subscribe({
      next: items => { this.companies = items; this.loading = false; },
      error: () => { this.error = 'Şirketler yüklenemedi.'; this.loading = false; }
    });
  }

  loadCities() {
    this.service.getCities().subscribe({ next: c => this.cities = c, error: () => {} });
  }

  setCity(city: string) {
    this.filterCity = this.filterCity === city ? '' : city;
    this.load();
  }

  search() { this.load(); }

  clearFilter() { this.filterCity = ''; this.filterSearch = ''; this.load(); }

  add() {
    if (!this.newItem.companyName?.trim()) { this.error = 'Şirket adı zorunludur.'; return; }
    this.service.create(this.newItem as CompanyListItem).subscribe({
      next: () => {
        this.success = 'Şirket eklendi.';
        this.newItem = { city: '', companyName: '', companyMail: '', companyWebsite: '', companyNumber: '' };
        this.showAddForm = false;
        this.load();
        this.loadCities();
      },
      error: () => { this.error = 'Eklenemedi.'; }
    });
  }

  startEdit(c: CompanyListItem) {
    this.editingId = c.id!;
    this.editItem = { ...c };
  }

  saveEdit(id: number) {
    this.service.update(id, this.editItem).subscribe({
      next: () => { this.editingId = null; this.load(); this.loadCities(); },
      error: () => { this.error = 'Güncellenemedi.'; }
    });
  }

  cancelEdit() { this.editingId = null; }

  delete(id: number) {
    if (!confirm('Bu şirketi silmek istiyor musunuz?')) return;
    this.service.delete(id).subscribe({ next: () => this.load() });
  }

  /** İnternalData JSON dosyasını içe aktar */
  onImportFile(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const json = JSON.parse(e.target?.result as string);
        // Dosya adından şehir çıkar: "ankaraITCompanies.json" → "Ankara"
        const cityFromFile = file.name.replace(/ITCompanies\.json$/i, '').replace(/Companies\.json$/i, '');
        const city = cityFromFile.charAt(0).toUpperCase() + cityFromFile.slice(1).toLowerCase();

        // JSON formatı: { companyData: [ { companyMail, parameters: { companyName, companyWebsite, companyNumber } } ] }
        const companyData: any[] = json.companyData || [];
        const companies = companyData.map((item: any) => ({
          companyName:    item.parameters?.companyName   || '',
          companyMail:    item.companyMail                || '',
          companyWebsite: item.parameters?.companyWebsite || '',
          companyNumber:  item.parameters?.companyNumber  || '',
          city
        }));

        this.service.bulkImport(city, companies).subscribe({
          next: res => {
            this.success = res.message || 'İçe aktarma başarılı';
            this.load();
            this.loadCities();
          },
          error: () => { this.error = 'İçe aktarma başarısız.'; }
        });
      } catch {
        this.error = 'JSON dosyası okunamadı.';
      }
    };
    reader.readAsText(file);
    input.value = '';
  }

  trackById(_: number, c: CompanyListItem) { return c.id; }
}
