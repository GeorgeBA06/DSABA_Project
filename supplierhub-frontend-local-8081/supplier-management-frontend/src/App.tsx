import { useEffect } from 'react';
import { AuthProvider, useAuth } from './core/auth';
import { usePathname } from './core/navigation';
import { findResource } from './core/resources';
import { AppLayout } from './ui/Layout';
import { ErrorBanner, Loading, PageHeading, SecondaryButton } from './ui/Shared';
import { AuthPage } from './pages/AuthPage';
import { Dashboard } from './pages/Dashboard';
import { Registry } from './pages/Registry';
import { Details } from './pages/Details';
import { CreateForm } from './pages/Forms';
import { Reports } from './pages/Reports';
import { ReceivingWorkflow } from './pages/Workflows';
import {OperationPage} from './pages/Operations';
function Routes() {
 const {path,go}=usePathname();const {status,can}=useAuth();
 useEffect(()=>{if(status==='signedIn'&&(path==='/'||path==='/login'))go('/dashboard');},[status,path]);
 if(status==='loading')return <div className="screen-loader"><Loading/></div>;
 if(status==='guest')return <AuthPage/>;
 const segments=path.split('/').filter(Boolean);const resource=findResource(segments[0]||'');
 let content;
 if(path==='/dashboard'||path==='/')content=<Dashboard go={go}/>;
 else if(segments[0]==='operation' && segments[1])content=<OperationPage key={path} kind={segments[1]} id={segments[2]} go={go}/>;
 else if(path==='/reports'&&can('PROCUREMENT_SPECIALIST','PROCUREMENT_MANAGER'))content=<Reports/>;
 else if(resource){
   if(!can(...resource.access))content=<ErrorBanner error="Недостаточно прав для просмотра раздела."/>;
   else if(segments.length===2&&segments[1]==='new')content=<CreateForm key={resource.id} id={resource.id} go={go}/>;
   else if(segments.length===3&&segments[2]==='receive'&&resource.id==='orders')content=<ReceivingWorkflow type="delivery" id={segments[1]} go={go}/>;
   else if(segments.length===3&&segments[2]==='inspect'&&resource.id==='deliveries')content=<ReceivingWorkflow type="inspection" id={segments[1]} go={go}/>;
   else if(segments.length===3&&segments[2]==='edit')content=<CreateForm key={`${resource.id}-${segments[1]}`} id={resource.id} editId={segments[1]} go={go}/>;
   else if(segments.length===2)content=<Details key={`${resource.id}-${segments[1]}`} resourceId={resource.id} id={segments[1]} go={go}/>;
   else content=<Registry key={resource.id} id={resource.id} go={go}/>;
 }else content=<><PageHeading title="Страница не найдена"/><section className="card"><p>Указанный раздел отсутствует.</p><SecondaryButton onClick={()=>go('/dashboard')}>На главную</SecondaryButton></section></>;
 return <AppLayout path={path} go={go}>{content}</AppLayout>;
}
export default function App(){return <AuthProvider><Routes/></AuthProvider>;}
