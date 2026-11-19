import { useEffect, useState } from 'react';
import { api } from '../core/api';
import {
  Badge,
  Empty,
  ErrorBanner,
  Icon,
  Loading,
  PageHeading,
  PrimaryButton,
  SecondaryButton,
} from '../ui/Shared';
import { formatDecimal } from '../core/navigation';
interface Row {
  supplierId: string;
  name: string;
  status: string;
  latestScore: number | null;
  band: string | null;
  ratingState: string | null;
}
export function Reports() {
  const [rows, setRows] = useState<Row[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [exportBusy, setExportBusy] = useState(false);
  const [exportError, setExportError] = useState<unknown>(null);
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    let alive = true;
    setLoading(true);
    setError(null);
    api
      .get<Row[]>('/reports/suppliers')
      .then((value) => {
        if (alive) setRows(value);
      })
      .catch((err) => {
        if (alive) setError(err);
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, [revision]);
  const exportXlsx = async () => {
    setExportBusy(true);
    setExportError(null);
    try {
      await api.download('/reports/suppliers.xlsx', 'отчёт-поставщики.xlsx');
    } catch (err) {
      setExportError(err);
    } finally {
      setExportBusy(false);
    }
  };
  return (
    <>
      <PageHeading
        title="Отчёт по поставщикам"
        subtitle="Текущие показатели надёжности и статусы сотрудничества"
        action={
          <div className="row wrap">
            <SecondaryButton onClick={() => setRevision((v) => v + 1)}>
              <Icon name="refresh" size={16} /> Обновить
            </SecondaryButton>
            <PrimaryButton busy={exportBusy} onClick={() => void exportXlsx()}>
              <Icon name="download" size={16} /> Экспорт в Excel
            </PrimaryButton>
          </div>
        }
      />
      <ErrorBanner error={error} /> <ErrorBanner error={exportError} />
      <section className="card">
        {loading ? (
          <Loading />
        ) : rows.length ? (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Поставщик</th>
                  <th>Статус</th>
                  <th>Последний балл</th>
                  <th>Категория</th>
                  <th>Достоверность</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.supplierId}>
                    <td>{row.name}</td>
                    <td>
                      <Badge value={row.status} />
                    </td>
                    <td>{formatDecimal(row.latestScore)}</td>
                    <td>
                      <Badge value={row.band} />
                    </td>
                    <td>
                      <Badge value={row.ratingState} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <Empty description="Данных для отчёта пока нет." />
        )}
      </section>
    </>
  );
}
