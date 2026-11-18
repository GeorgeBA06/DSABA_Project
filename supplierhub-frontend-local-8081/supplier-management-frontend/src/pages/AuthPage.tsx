import { useState } from 'react';
import type { FormEvent } from 'react';
import { useAuth } from '../core/auth';
import { ErrorBanner, Field, Icon, PrimaryButton, TextInput } from '../ui/Shared';
export function AuthPage() {
 const {login}=useAuth();const [name,setName]=useState('');const [password,setPassword]=useState('');const [busy,setBusy]=useState(false);const [error,setError]=useState<unknown>(null);
 const submit=async(e:FormEvent)=>{e.preventDefault();setBusy(true);setError(null);try{await login(name.trim(),password);}catch(err){setError(err);}finally{setBusy(false);}};
 return <main className="auth-screen"><section className="auth-card"><div className="auth-icon"><Icon name="package" size={28}/></div><h1>Вход в систему</h1><p className="muted">Управление поставщиками и оценка их надёжности</p><form onSubmit={submit} className="form-stack"><Field label="Логин"><TextInput name="login" value={name} onChange={setName} required placeholder="Логин сотрудника"/></Field><Field label="Пароль"><TextInput name="password" value={password} onChange={setPassword} required type="password" placeholder="Пароль"/></Field>{error&&<ErrorBanner error={error}/>}<PrimaryButton type="submit" busy={busy}>Войти</PrimaryButton></form><div className="auth-note">Учётные записи создаёт администратор. Публичная регистрация недоступна.</div></section></main>;
}
