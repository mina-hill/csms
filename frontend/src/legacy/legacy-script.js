// ════════════════════════════════════════════════════
//  STATE
// ════════════════════════════════════════════════════
var flocks    = [];
var suppliers = [
  {id:'SUP-001',name:'Golden Poultry Farms',type:'Chicks/Flock',contact:'+92 300 123 4567',address:'Lahore',added:'2025-01-10'},
  {id:'SUP-002',name:'Agri-Pro Hatcheries', type:'Feed',         contact:'info@agripro.pk',  address:'Gujranwala',added:'2025-01-15'}
];
var mortalities  = [];   // {id,flockId,date,day,night,count,type,notes,time,editor}
var weights      = [];   // {id,flockId,date,weight,sample,notes,time,editor}
var weeklyRecords = [];  // {id,flockId,date,ageDays,totalChicks,totalMortality,remainingChicks,feedUsedKg,avgWeightG,fcr,notes,time,editor}
var flockSales   = [];   // {id,flockId,date,buyer,qtyKg,vehicle,emptyW,loadedW,netW,rate,gross,less,commission,total,notes,time,editor}
var otherSales   = [];   // {id,date,category,desc,buyer,amount,time,editor}
var feedStock    = {};   // {type:{qty,threshold,lastPurchase,lastUsage}}
var feedTxns     = [];   // {id,type,feedType,flock,buyer,qty,cost,date,time}
var medStock     = {};   // {name:{qty,threshold,supplier,lastUpdated}}
var medTxns      = [];   // {id,type,medicine,flock,sup,qty,cost,date,time}
var dailyFeedRecords = []; // {id,flockId,date,feedType,daySacks,nightSacks,totalSacks,time,editor}
var dailyMedRecords  = []; // {id,flockId,date,items:[{name,qty,unit,usageTime}],notes,time,editor}
var bradaStock   = 0;
var bradaTxns    = [];   // {id,type,flock,sup,qty,cost,date,time,balance}
var expenses     = [];   // {id,cat,desc,units,rate,amount,date,flockId,time}
var workers      = [];   // {id,name,role,contact,join,salary,status}
var payrollRuns  = [];   // {id,month,year,workers:[{name,salary}],total,processedOn,status}
var weeklySummaries = []; // rows from GET /weekly-summary (weekly_summary / fcr_value)
var currentReportTab = 'mortality';
var payrollSelectedWorkerId = '';
var auditLog     = [];
var flockLoadWarned = false, feedLoadWarned = false, medicineLoadWarned = false;
var flockSeq=1, supSeq=3, morSeq=1, wtSeq=1, fsSeq=1, osSeq=1, ftSeq=1, mdSeq=1, bbSeq=1, expSeq=1, wkSeq=1, prSeq=1;
var editTarget=null, closeTarget=null, viewTarget=null, supEditTarget=null, workerEditTarget=null;
var EDITOR = 'Shed Manager';
var API_BASE = '/api';

function api(path, opts){
  return fetch(API_BASE + path, opts || {});
}

// ════════════════════════════════════════════════════
//  HELPERS
// ════════════════════════════════════════════════════
var $=function(id){return document.getElementById(id);};
function today(){var d=new Date();return d.getFullYear()+'-'+pad(d.getMonth()+1)+'-'+pad(d.getDate());}
function pad(n){return String(n).padStart(2,'0');}
function fmt(s){if(!s)return '—';return new Date(s+'T00:00:00').toLocaleDateString('en-GB',{day:'2-digit',month:'short',year:'numeric'});}
function now(){var d=new Date();return d.toLocaleDateString('en-GB',{day:'2-digit',month:'short',year:'numeric'})+' '+pad(d.getHours())+':'+pad(d.getMinutes());}
function esc(s){return String(s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');}
function rupees(n){return '₨'+Number(n||0).toLocaleString();}
function inv(id,s){var e=$(id);if(!e)return;s?e.classList.add('inv'):e.classList.remove('inv');}
function toast(msg,cls){var t=document.createElement('div');t.className='toast '+(cls||'t-ok');t.innerHTML='<span>'+msg+'</span>';$('toasts').appendChild(t);setTimeout(function(){t.remove();},3200);}
function openM(id){$(id).classList.add('on');}
function closeM(id){$(id).classList.remove('on');}
function nextFid(){return 'FLK-'+String(flockSeq++).padStart(3,'0');}
function activeFlocks(){return flocks.filter(function(f){return f.status==='Active';});}
function fillFlockSelect(selId){
  var sel=$(selId);sel.innerHTML='<option value="">— Select Active Flock —</option>';
  activeFlocks().forEach(function(f){var o=document.createElement('option');o.value=f.id;o.textContent=f.id+' — '+f.breed;sel.appendChild(o);});
}
function fillSupSelect(selId){
  var sel=$(selId);sel.innerHTML='<option value="">— Select supplier —</option>';
  var types=arguments.length>1?arguments[1]:null;
  suppliers
    .filter(function(s){return !types||!types.length||types.indexOf(s.type)>=0;})
    .forEach(function(s){var o=document.createElement('option');o.value=s.id;o.textContent=s.name;sel.appendChild(o);});
}

function normalizeSupplierType(t){
  if(!t)return 'Other';
  var x=String(t).trim();
  var u=x.toUpperCase().replace(/-/g,'_');
  if(u==='CHICKS')return 'Chicks Supplier';
  if(u==='FEED')return 'Feed Supplier';
  if(u==='MEDICINE')return 'Medicine Supplier';
  if(u==='EQUIPMENT')return 'Equipment Supplier';
  if(u==='BRADA'||u==='OTHER')return 'Other';
  if(x==='Chicks/Flock'||x==='Chicks'||x==='Flock')return 'Chicks Supplier';
  if(x==='Feed')return 'Feed Supplier';
  if(x==='Medicine')return 'Medicine Supplier';
  if(x==='Equipment')return 'Equipment Supplier';
  if(x==='General')return 'Other';
  if(x==='Brada')return 'Other';
  return x;
}

function uiTypeToSupplierTypeEnum(label){
  var map={
    'Chicks Supplier':'CHICKS',
    'Feed Supplier':'FEED',
    'Medicine Supplier':'MEDICINE',
    'Equipment Supplier':'EQUIPMENT',
    'Other':'OTHER'
  };
  return map[label] || 'OTHER';
}
suppliers.forEach(function(s){s.type=normalizeSupplierType(s.type);});

function mapSupplierFromApi(s){
  var rawId = s.id != null ? s.id : s.supplierId;
  return {
    id: rawId,
    name: s.name || '',
    type: normalizeSupplierType(s.supplierType || s.type),
    contact: s.phone || s.contact || '',
    address: s.address || '',
    added: (s.createdAt || '').slice(0,10) || today(),
    isActive: s.isActive !== false
  };
}

function mapWorkerFromApi(w){
  var rawId = w.id != null ? w.id : w.workerId;
  return {
    id: rawId,
    name: w.name || '',
    role: w.role || '',
    contact: w.contact || '',
    join: w.joinDate || '',
    salary: Number(w.salaryRate != null ? w.salaryRate : 0),
    status: w.isActive === false ? 'Inactive' : 'Active'
  };
}

function toArrayPayload(payload){
  if(Array.isArray(payload)) return payload;
  if(payload && Array.isArray(payload.content)) return payload.content;
  if(payload && Array.isArray(payload.data)) return payload.data;
  return [];
}

function normalizeFlockStatus(status){
  var key = String(status || '').toUpperCase();
  return key === 'CLOSED' ? 'Closed' : 'Active';
}

function mapFlockFromApi(f){
  var supplier = suppliers.find(function(s){ return String(s.id) === String(f.supplierId); });
  return {
    id: String(f.flockCode || f.id || f.flockId || ''),
    flockDbId: String(f.id || f.flockId || ''),
    breed: f.breed || '',
    qty: Number(f.currentQty != null ? f.currentQty : f.initialQty || 0),
    origQty: Number(f.initialQty || 0),
    arrivalDate: f.arrivalDate || '',
    supplierName: supplier ? supplier.name : '',
    supId: f.supplierId != null ? String(f.supplierId) : '',
    notes: f.notes || '',
    status: normalizeFlockStatus(f.status),
    closeDate: f.closeDate || ''
  };
}

function hydrateFeedStockFromApi(items){
  var next = {};
  (items || []).forEach(function(it){
    if(!it || !it.name) return;
    next[it.name] = {
      id: String(it.id || it.feedTypeId || ''),
      qty: Number(it.currentStock || 0),
      threshold: Number(it.minThreshold != null ? it.minThreshold : 0),
      lastPurchase: it.lastUpdated ? String(it.lastUpdated).slice(0,10) : null,
      lastUsage: it.lastUpdated ? String(it.lastUpdated).slice(0,10) : null
    };
  });
  feedStock = next;
}

function hydrateMedStockFromApi(items){
  var next = {};
  (items || []).forEach(function(it){
    if(!it || !it.name) return;
    next[it.name] = {
      id: String(it.id || it.medicineId || ''),
      qty: Number(it.currentStock || 0),
      threshold: Number(it.minThreshold != null ? it.minThreshold : 0),
      supplier: '',
      supId: '',
      lastUpdated: it.lastUpdated ? String(it.lastUpdated).slice(0,10) : null,
      unit: it.unit || it.unitType || 'units'
    };
  });
  medStock = next;
}

function flockByUiId(uiId){
  return flocks.find(function(f){ return String(f.id)===String(uiId); });
}
function flockDbUuid(uiId){
  var f=flockByUiId(uiId);
  return f && f.flockDbId ? String(f.flockDbId) : null;
}
function isLikelyUuid(s){
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(String(s||''));
}
function flockUiIdFromDbUuid(uuid){
  if(!uuid)return '';
  var u=String(uuid);
  var f=flocks.find(function(x){ return String(x.flockDbId)===u; });
  return f ? f.id : '';
}
function feedTypeNameById(id){
  id=String(id||'');
  var keys=Object.keys(feedStock);
  for(var i=0;i<keys.length;i++){
    var ft=feedStock[keys[i]];
    if(ft&&String(ft.id)===id)return keys[i];
  }
  return '';
}
function medicineNameById(id){
  id=String(id||'');
  var keys=Object.keys(medStock);
  for(var i=0;i<keys.length;i++){
    var m=medStock[keys[i]];
    if(m&&String(m.id)===id)return keys[i];
  }
  return '';
}
function applyMortalityCumulative(){
  var byFlock={};
  mortalities.slice().sort(function(a,b){ return (a.date||'').localeCompare(b.date||''); }).forEach(function(m){
    byFlock[m.flockId]=(byFlock[m.flockId]||0)+(m.count||0);
    m.cumulative=byFlock[m.flockId];
  });
}
async function loadMortalityFromApi(){
  try{
    var res=await api('/mortality');
    if(!res.ok)return;
    var rows=toArrayPayload(await res.json());
    var groups={};
    rows.forEach(function(m){
      var fid=flockUiIdFromDbUuid(m.flockId);
      if(!fid)return;
      var d=(m.recordDate||'').toString().slice(0,10);
      var typ=(m.type||'Recorded').toString();
      var k=fid+'|'+d+'|'+typ;
      if(!groups[k]){
        groups[k]={id:'MRT-'+k,flockId:fid,date:d,day:0,night:0,count:0,type:typ,notes:'',time:now(),editor:EDITOR,cumulative:0};
      }
      var c=Number(m.count||0);
      var sh=String(m.shift||'').toUpperCase();
      if(sh==='DAY')groups[k].day+=c;
      else if(sh==='NIGHT')groups[k].night+=c;
      else groups[k].day+=c;
      groups[k].count=groups[k].day+groups[k].night;
    });
    mortalities=Object.keys(groups).map(function(k){ return groups[k]; });
    applyMortalityCumulative();
  }catch(_e){}
}
async function loadWeightsFromApi(){
  try{
    var res=await api('/weights');
    if(!res.ok)return;
    var rows=toArrayPayload(await res.json());
    weeklyRecords=rows.map(function(w){
      var fid=flockUiIdFromDbUuid(w.flockId);
      if(!fid)return null;
      var date=(w.recordDate||'').toString().slice(0,10);
      var avgG=w.avgWeightKg!=null?Number(w.avgWeightKg)*1000:0;
      var age=chickAgeDays(fid,date);
      var ft=flockByUiId(fid);
      var totalChicks=ft?ft.origQty:0;
      var totalMort=totalMortalityForFlockThrough(fid,date);
      var remaining=Math.max(0,totalChicks-totalMort);
      var cumSacks=cumulativeFeedSacksThrough(fid,date);
      var feedKg=cumSacks*50;
      var liveKg=remaining*(avgG/1000);
      var fcr=fcrDbStyleFromMetrics(cumSacks,(avgG/1000),remaining);
      return {
        id:w.weightId||w.id,
        flockId:fid,
        date:date,
        ageDays:age,
        totalChicks:totalChicks,
        totalMortality:totalMort,
        remainingChicks:remaining,
        feedUsedKg:feedKg,
        avgWeightG:avgG,
        fcr:fcr,
        notes:'',
        time:now(),
        editor:EDITOR
      };
    }).filter(function(r){ return r!=null; });
  }catch(_e){}
}
async function loadFeedTxnsFromApi(){
  try{
    var purRes=await api('/feed/purchases');
    var useRes=await api('/feed/usage');
    var salRes=await api('/feed/sales');
    var pur=purRes.ok ? toArrayPayload(await purRes.json()) : [];
    var use=useRes.ok ? toArrayPayload(await useRes.json()) : [];
    var sal=salRes.ok ? toArrayPayload(await salRes.json()) : [];
    var next=[];
    pur.forEach(function(p){
      var name=feedTypeNameById(p.feedTypeId);
      var qty=p.sackCount!=null?p.sackCount:p.sacksQty;
      var sup=suppliers.find(function(s){ return String(s.id)===String(p.supplierId); });
      next.push({id:'FP-'+p.purchaseId,type:'Purchase',feedType:name||String(p.feedTypeId),supId:p.supplierId,supName:sup?sup.name:'',qty:qty,cost:Number(p.totalCost||0),costPerSack:Number(p.costPerSack||0),date:(p.purchaseDate||'').toString().slice(0,10),time:now()});
    });
    use.forEach(function(u){
      var name=feedTypeNameById(u.feedTypeId);
      var fid=flockUiIdFromDbUuid(u.flockId);
      var udate=(u.usageDate||u.recordDate||'').toString().slice(0,10);
      next.push({id:'FU-'+u.usageId,type:'Usage',feedType:name||String(u.feedTypeId),flockId:fid,flockName:'',shift:(u.shift||''),qty:u.sacksUsed,date:udate,time:now()});
    });
    sal.forEach(function(s){
      var sacks=s.sacksSold!=null?s.sacksSold:s.sacksQty;
      next.push({id:'FSL-'+s.saleId,type:'Sale',feedType:'Sacks Sale',buyer:s.buyerName,qty:sacks,cost:Number(s.totalRevenue||0),date:(s.saleDate||'').toString().slice(0,10),time:now()});
    });
    feedTxns=next;
  }catch(_e){}
}
async function loadMedTxnsFromApi(){
  try{
    var purRes=await api('/medicine/purchases');
    var useRes=await api('/medicine/usage');
    if(!purRes.ok||!useRes.ok)return;
    var pur=toArrayPayload(await purRes.json());
    var use=toArrayPayload(await useRes.json());
    var next=[];
    pur.forEach(function(p){
      var name=medicineNameById(p.medicineId);
      var sup=suppliers.find(function(s){ return String(s.id)===String(p.supplierId); });
      next.push({id:'MP-'+p.purchaseId,type:'Purchase',medicine:name||String(p.medicineId),supId:p.supplierId,supName:sup?sup.name:'',qty:p.quantity,cost:Number(p.totalCost||0),unit:p.unit||'',date:(p.purchaseDate||'').toString().slice(0,10),time:now()});
    });
    use.forEach(function(u){
      var name=medicineNameById(u.medicineId);
      var fid=flockUiIdFromDbUuid(u.flockId);
      var udate=(u.usageDate||u.recordDate||'').toString().slice(0,10);
      next.push({id:'MU-'+u.usageId,type:'Usage',medicine:name||String(u.medicineId),flockId:fid,qty:Number(u.dosage||0),unit:u.unit||'',usageTime:u.usageTime||'',notes:u.notes||'',date:udate,time:now()});
    });
    medTxns=next;
  }catch(_e){}
}
function mapFlockSaleFromApi(s){
  var fid=flockUiIdFromDbUuid(s.flockId);
  var qty=Number(s.qtySold||0);
  var wpb=Number(s.weightPerBirdKg||0);
  var ppk=Number(s.pricePerKg||0);
  var netW=qty*wpb;
  var total=Number(s.totalAmount||0);
  return {
    id:s.saleId||s.id,
    flockId:fid,
    date:(s.saleDate||'').toString().slice(0,10),
    buyer:s.buyerName||'',
    qtyKg:qty,
    vehicle:'—',
    emptyW:0,
    loadedW:0,
    netW:netW,
    rate:ppk,
    gross:netW*ppk,
    less:0,
    commission:0,
    total:total,
    notes:'',
    time:now(),
    editor:EDITOR
  };
}
async function loadFlockSalesFromApi(){
  try{
    var res=await api('/sales/flock');
    if(!res.ok)return;
    var rows=toArrayPayload(await res.json());
    flockSales=rows.map(mapFlockSaleFromApi);
  }catch(_e){}
}
function expenseEnumToUi(cat){
  var map={
    UTILITY:'Utilities (legacy)',ELECTRICITY:'Electricity',WATER:'Water',GAS:'Gas (utility)',
    FUEL:'Fuel (vehicle)',LABOR:'Labour',MAINTENANCE:'Maintenance',MISCELLANEOUS:'Miscellaneous'
  };
  var k=(cat||'').toString().toUpperCase();
  return map[k]||cat||'Miscellaneous';
}
function uiExpenseToEnum(cat){
  var c=(cat||'').trim().toLowerCase().replace(/\s+/g,' ');
  if(!c)return 'MISCELLANEOUS';
  var exact={
    electricity:'ELECTRICITY',water:'WATER',
    gas:'GAS','natural gas':'GAS',lpg:'GAS',
    fuel:'FUEL',diesel:'FUEL',petrol:'FUEL',gasoline:'FUEL',
    labour:'LABOR',labor:'LABOR',
    maintenance:'MAINTENANCE',miscellaneous:'MISCELLANEOUS',
    expense:'MISCELLANEOUS',other:'MISCELLANEOUS',general:'MISCELLANEOUS'
  };
  if(exact[c])return exact[c];
  if(/\b(diesel|petrol|gasoline|fuel)\b/.test(c))return 'FUEL';
  if(/\b(gas|lpg)\b/.test(c)||c.indexOf('natural gas')>=0)return 'GAS';
  if(c.indexOf('labour')>=0||c.indexOf('labor')>=0)return 'LABOR';
  if(c.indexOf('maintenance')>=0)return 'MAINTENANCE';
  if(c.indexOf('electricity')>=0)return 'ELECTRICITY';
  return 'MISCELLANEOUS';
}
function mapExpenseFromApi(e){
  return {
    id:e.expenseId||e.id,
    cat:expenseEnumToUi(e.category),
    desc:e.description||'',
    units:1,
    rate:Number(e.amount||0),
    amount:Number(e.amount||0),
    date:(e.expenseDate||'').toString().slice(0,10),
    flockId:e.flockId?flockUiIdFromDbUuid(e.flockId):'',
    time:now(),
    editor:EDITOR
  };
}
async function loadExpensesFromApi(){
  try{
    var res=await api('/expenses');
    if(!res.ok)return;
    var rows=toArrayPayload(await res.json());
    expenses=rows.map(mapExpenseFromApi);
  }catch(_e){}
}
async function loadBradaFromApi(){
  try{
    var res=await api('/brada/purchases');
    if(!res.ok)return;
    var rows=toArrayPayload(await res.json());
    bradaTxns=rows.map(function(p){
      var sup=suppliers.find(function(s){ return String(s.id)===String(p.supplierId); });
      return {
        id:'BT-'+p.purchaseId,
        type:'Purchase',
        flockId:flockUiIdFromDbUuid(p.flockId),
        supId:p.supplierId,
        supName:sup?sup.name:'',
        qty:p.quantity,
        cost:Number(p.totalCost||0),
        date:(p.purchaseDate||'').toString().slice(0,10),
        time:now()
      };
    });
  }catch(_e){}
}
function mapSystemAuditFromApi(a){
  var et=a.entityType||a.tableName||'system';
  var fidDb=a.flockId||null;
  var flockUi=fidDb?flockUiIdFromDbUuid(fidDb):'';
  var label=(flockUi?(flockUi+' · '):'')+et+' / '+(a.action||'');
  return {
    module:label,
    flockUi:flockUi,
    action:a.action||'',
    detail:(a.details||'').slice(0,800),
    diff:[],
    time:a.loggedAt?new Date(a.loggedAt).toLocaleString('en-GB'):now(),
    editor:'System'
  };
}
async function loadSystemAuditFromApi(){
  try{
    var res=await api('/audit');
    if(!res.ok)return;
    var rows=toArrayPayload(await res.json());
    auditLog=rows.map(mapSystemAuditFromApi);
  }catch(_e){}
}

function uiOtherSaleCategoryToApi(cat){
  var map = {
    'Manure':'MANURE',
    'Equipment':'EQUIPMENT',
    'By-product':'BY_PRODUCT',
    'Brada':'BRADA',
    'Other':'OTHER'
  };
  return map[cat] || 'OTHER';
}

function apiOtherSaleCategoryToUi(cat){
  var map = {
    'MANURE':'Manure',
    'EQUIPMENT':'Equipment',
    'BY_PRODUCT':'By-product',
    'SACKS_SALE':'Sacks Sale',
    'BRADA':'Brada',
    'OTHER':'Other'
  };
  var key = (cat || '').toUpperCase();
  return map[key] || (cat || 'Other');
}

function mapOtherSaleFromApi(s){
  return {
    id: s.id || s.saleId,
    date: (s.saleDate || '').toString().slice(0,10),
    category: apiOtherSaleCategoryToUi(s.category),
    desc: s.description || '',
    buyer: s.buyerName != null ? s.buyerName : (s.buyer || ''),
    amount: Number(s.amount || 0),
    time: s.createdAt || now(),
    editor: EDITOR
  };
}

function mapPayrollReportFromApi(r){
  return {
    id: 'PR-' + String(r.periodYear) + '-' + String(r.periodMonth) + '-' + String(r.daysWorked || 0),
    start: r.startDate,
    end: r.endDate,
    days: r.daysWorked,
    workers: (r.workers || []).map(function(w){
      return {
        name: w.workerName,
        role: w.role,
        monthly: Number(w.monthlySalary || 0),
        perDay: Number(w.dailyRate || 0),
        days: w.daysWorked,
        total: Number(w.amount || 0)
      };
    }),
    total: Number(r.totalAmount || 0),
    processedOn: r.processedAt ? new Date(r.processedAt).toLocaleString('en-GB') : now(),
    status: r.status || 'Processed'
  };
}

async function loadOtherSalesFromApi(){
  try{
    var res = await api('/sales/other');
    if(!res.ok) res = await api('/other-sales');
    if(!res.ok)return;
    var dbSales = toArrayPayload(await res.json());
    var mapped = dbSales.map(mapOtherSaleFromApi);
    var localOnly = otherSales.filter(function(s){ return String(s.id||'').indexOf('OS-')===0; });
    otherSales = mapped.concat(localOnly);
  }catch(_err){
    // Keep local state silently when backend is unavailable.
  }
}

async function loadFlocksFromApi(){
  try{
    var res = await api('/flocks');
    if(!res.ok){
      if(!flockLoadWarned){
        flockLoadWarned = true;
        toast('Could not load flocks from backend ('+res.status+').','t-bad');
      }
      return;
    }
    var data = toArrayPayload(await res.json());
    var mappedFlocks = data.map(mapFlockFromApi);
    flocks = mappedFlocks;
    flockSeq = flocks.length + 1;
    flockLoadWarned = false;
  }catch(_err){
    if(!flockLoadWarned){
      flockLoadWarned = true;
      toast('Backend unavailable: could not load flocks.','t-bad');
    }
  }
}

async function loadFeedTypesFromApi(){
  try{
    var res = await api('/feed-types');
    if(!res.ok){
      if(!feedLoadWarned){
        feedLoadWarned = true;
        toast('Could not load feed types from backend ('+res.status+').','t-bad');
      }
      return;
    }
    var data = toArrayPayload(await res.json());
    hydrateFeedStockFromApi(data);
    refreshFeedTypeSelects();
    feedLoadWarned = false;
  }catch(_err){
    if(!feedLoadWarned){
      feedLoadWarned = true;
      toast('Backend unavailable: could not load feed inventory.','t-bad');
    }
  }
}

async function loadMedicinesFromApi(){
  try{
    var res = await api('/medicines');
    if(!res.ok){
      if(!medicineLoadWarned){
        medicineLoadWarned = true;
        toast('Could not load medicines from backend ('+res.status+').','t-bad');
      }
      return;
    }
    var data = toArrayPayload(await res.json());
    hydrateMedStockFromApi(data);
    refreshMedSelects();
    medicineLoadWarned = false;
  }catch(_err){
    if(!medicineLoadWarned){
      medicineLoadWarned = true;
      toast('Backend unavailable: could not load medicine inventory.','t-bad');
    }
  }
}

async function loadPayrollRunsFromApi(){
  try{
    var res = await api('/payroll/report');
    if(!res.ok)return;
    var data = await res.json();
    payrollRuns = data.map(mapPayrollReportFromApi);
  }catch(_err){
    // Keep local state silently when backend is unavailable.
  }
}

async function loadWeeklySummariesFromApi(){
  try{
    var res=await api('/weekly-summary');
    if(!res.ok)return;
    var rows=toArrayPayload(await res.json());
    weeklySummaries=rows.map(function(s){
      return {
        summaryId:s.summaryId,
        flockDbId:String(s.flockId||''),
        weekNumber:Number(s.weekNumber||0),
        weekEndDate:(s.weekEndDate||'').toString().slice(0,10),
        remainingChicks:Number(s.remainingChicks!=null?s.remainingChicks:0),
        cumulativeFeedSacks:Number(s.cumulativeFeedSacks!=null?s.cumulativeFeedSacks:0),
        avgWeightKg:s.avgWeightKg!=null?Number(s.avgWeightKg):null,
        fcrValue:s.fcrValue!=null?Number(s.fcrValue):null
      };
    });
  }catch(_e){
    weeklySummaries=[];
  }
}

async function loadInitialData(){
  try{
    var supplierRes = await api('/suppliers');
    if(supplierRes.ok){
      var supplierData = await supplierRes.json();
      suppliers = supplierData.map(mapSupplierFromApi);
      supSeq = suppliers.length + 1;
    }
  }catch(_err){
    toast('Using local supplier data (backend unavailable).','t-info');
  }

  try{
    var workerRes = await api('/workers');
    if(workerRes.ok){
      var workerData = await workerRes.json();
      workers = workerData.map(mapWorkerFromApi);
      wkSeq = workers.length + 1;
    }
  }catch(_err){
    toast('Using local worker data (backend unavailable).','t-info');
  }

  await loadFlocksFromApi();
  await loadSystemAuditFromApi();
  await loadFeedTypesFromApi();
  await loadFeedTxnsFromApi();
  await loadMortalityFromApi();
  await loadMedicinesFromApi();
  await loadMedTxnsFromApi();
  await loadWeightsFromApi();
  await loadBradaFromApi();
  await loadFlockSalesFromApi();
  await loadOtherSalesFromApi();
  await loadExpensesFromApi();
  await loadPayrollRunsFromApi();
  await loadWeeklySummariesFromApi();
}

function daysBetween(start,end){
  if(!start||!end)return null;
  var a=new Date(start+'T00:00:00'),b=new Date(end+'T00:00:00');
  var ms=b.getTime()-a.getTime();
  return Math.floor(ms/86400000);
}
function chickAgeDays(flockId,recordDate){
  var f=flocks.find(function(x){return x.id===flockId;});
  if(!f||!f.arrivalDate||!recordDate)return null;
  var d=daysBetween(f.arrivalDate,recordDate);
  return d===null?null:(d+1);
}
function totalMortalityForFlock(flockId){
  return mortalities.filter(function(m){return m.flockId===flockId;}).reduce(function(s,m){return s+(m.count||0);},0);
}
function remainingChicksForFlock(flockId){
  var f=flocks.find(function(x){return x.id===flockId;});
  if(!f)return null;
  return Math.max(0,(f.origQty||0)-totalMortalityForFlock(flockId));
}
function feedSacksForFlockBetween(flockId,startDate,endDate){
  if(!flockId||!startDate||!endDate)return 0;
  return feedTxns.filter(function(t){
    return t.type==='Usage'&&t.flockId===flockId&&t.date&&t.date>=startDate&&t.date<=endDate;
  }).reduce(function(s,t){return s+(t.qty||0);},0);
}
function cumulativeFeedSacksThrough(flockId,endDate){
  if(!flockId||!endDate)return 0;
  return feedTxns.filter(function(t){
    return t.type==='Usage'&&t.flockId===flockId&&t.date&&t.date<=endDate;
  }).reduce(function(s,t){return s+(t.qty||0);},0);
}
function weekNumberForFlockDate(flockId,recordDate){
  var age=chickAgeDays(flockId,recordDate);
  if(age===null||age<1)return 1;
  return Math.max(1,Math.ceil(age/7));
}
function feedUsedKgForFlockBetween(flockId,startDate,endDate){
  return feedSacksForFlockBetween(flockId,startDate,endDate)*50;
}
function totalMortalityForFlockThrough(flockId,endDate){
  if(!flockId||!endDate)return 0;
  var end=String(endDate).slice(0,10);
  return mortalities.filter(function(m){
    return m.flockId===flockId && m.date && String(m.date).slice(0,10)<=end;
  }).reduce(function(s,m){return s+(m.count||0);},0);
}
function fcrDbStyleFromMetrics(cumSacks,avgWeightKg,remainingChicks){
  var cs=Number(cumSacks||0),aw=Number(avgWeightKg||0),rm=Number(remainingChicks||0);
  if(cs<=0||aw<=0||rm<=0)return null;
  var feedKg=cs*50;
  var liveKg=rm*aw;
  if(feedKg<=0||liveKg<=0)return null;
  return feedKg/liveKg;
}
function totalSackSaleRevenue(){
  return feedTxns.filter(function(t){return t.type==='Sale';}).reduce(function(s,t){return s+(t.cost||0);},0);
}
function isPayrollMirrorExpense(e){
  return e && e.cat==='Labour' && /^Payroll —/i.test(String(e.desc||''));
}
function labourExpenseForPnL(){
  return expenses.filter(function(e){return e.cat==='Labour' && !isPayrollMirrorExpense(e);}).reduce(function(s,e){return s+e.amount;},0);
}
function nonLabourExpenseForPnL(){
  return expenses.filter(function(e){return e.cat!=='Labour';}).reduce(function(s,e){return s+e.amount;},0);
}
function monthName(m){return ['','January','February','March','April','May','June','July','August','September','October','November','December'][m];}
function totalRevenue(){
  return flockSales.reduce(function(s,x){return s+x.total;},0)+otherSales.reduce(function(s,x){return s+x.amount;},0)+totalSackSaleRevenue();
}

// ════════════════════════════════════════════════════
//  NAVIGATION
// ════════════════════════════════════════════════════
document.querySelectorAll('.nav-item').forEach(function(btn){
  btn.addEventListener('click',function(){
    document.querySelectorAll('.nav-item').forEach(function(b){b.classList.remove('on');});
    document.querySelectorAll('.view').forEach(function(v){v.classList.remove('on');});
    btn.classList.add('on');
    var v=btn.getAttribute('data-view');
    $('view-'+v).classList.add('on');
    var render={dashboard:renderDash,flocks:function(){loadFlocksFromApi().then(renderFlocks);},daily:function(){Promise.all([loadMortalityFromApi(),loadFeedTxnsFromApi(),loadMedTxnsFromApi(),loadWeightsFromApi()]).then(renderDaily);},sales:function(){Promise.all([loadFlockSalesFromApi(),loadOtherSalesFromApi(),loadFeedTxnsFromApi()]).then(renderSalesView);},
      feed:function(){Promise.all([loadFeedTypesFromApi(),loadFeedTxnsFromApi()]).then(renderFeed);},medicine:function(){Promise.all([loadMedicinesFromApi(),loadMedTxnsFromApi()]).then(renderMedicine);},brada:function(){loadBradaFromApi().then(renderBrada);},expenses:function(){loadExpensesFromApi().then(renderExpenses);},
      payroll:renderPayroll,suppliers:renderSuppliers,audit:function(){loadSystemAuditFromApi().then(renderAudit);}};
    if(v==='reports'){
      currentReportTab=(document.querySelector('.tab-pill.on[data-rtab]')&&document.querySelector('.tab-pill.on[data-rtab]').getAttribute('data-rtab'))||currentReportTab||'mortality';
      Promise.all([loadWeeklySummariesFromApi(),loadFeedTxnsFromApi(),loadMortalityFromApi(),loadWeightsFromApi(),loadFlockSalesFromApi(),loadOtherSalesFromApi(),loadExpensesFromApi(),loadPayrollRunsFromApi(),loadMedTxnsFromApi(),loadBradaFromApi()]).then(function(){
        renderReport(currentReportTab);
      });
    }else if(render[v])render[v]();
  });
});
document.addEventListener('click',function(e){
  var cb=e.target.closest('[data-close]');if(cb){closeM(cb.getAttribute('data-close'));return;}
  if(e.target.classList.contains('overlay'))closeM(e.target.id);
});

// ════════════════════════════════════════════════════
//  AUDIT LOG
// ════════════════════════════════════════════════════
function addLog(module,action,detail,diff){
  auditLog.unshift({module:module,action:action,detail:detail||'',diff:diff||[],time:now(),editor:EDITOR,_local:true});
  if(document.getElementById('view-dashboard').classList.contains('on'))renderDash();
}
function dotCls(a){
  if(a==='Registered'||a==='Added')return 'log-dot ld-reg';
  if(a==='Updated'||a==='Edited')return 'log-dot ld-upd';
  if(a==='Closed'||a==='Deleted')return 'log-dot ld-cls';
  return 'log-dot ld-gen';
}
function buildLogHtml(entries){
  if(!entries||!entries.length)return '<div class="empty"><div class="ei">📋</div><div class="et">No records yet.</div></div>';
  return entries.map(function(e){
    var diffHtml='';
    if(e.diff&&e.diff.length){
      diffHtml='<table class="diff-tbl" style="margin-top:6px"><thead><tr><th>Field</th><th>Previous</th><th>New</th></tr></thead><tbody>'
        +e.diff.map(function(d){return '<tr><td style="font-weight:600">'+esc(d.field)+'</td><td><span class="ov">'+esc(String(d.old))+'</span></td><td><span class="nv">'+esc(String(d.nw))+'</span></td></tr>';}).join('')
        +'</tbody></table>';
    }
    return '<div class="log-entry"><div class="'+dotCls(e.action)+'"></div>'
      +'<div class="log-body"><div class="log-act"><strong>'+esc(e.module)+'</strong> — '+esc(e.action)+'</div>'
      +(e.detail?'<div class="log-det">'+esc(e.detail)+'</div>':'')
      +diffHtml+'<div class="log-who">by <strong>'+esc(e.editor)+'</strong></div></div>'
      +'<div class="log-time">'+e.time+'</div></div>';
  }).join('');
}
function renderAudit(){var el=$('audit-log');if(el)el.innerHTML=buildLogHtml(auditLog);}

// ════════════════════════════════════════════════════
//  DASHBOARD
// ════════════════════════════════════════════════════
function renderDash(){
  var active=activeFlocks();
  $('s-active').textContent=active.length;
  $('s-closed').textContent=flocks.filter(function(f){return f.status==='Closed';}).length;
  $('s-birds').textContent=active.reduce(function(s,f){return s+f.qty;},0).toLocaleString();
  $('s-revenue').textContent=rupees(totalRevenue());
  $('dash-log').innerHTML=buildLogHtml(auditLog.slice(0,8));
  // Alerts
  var alerts=[];
  Object.keys(feedStock).forEach(function(t){var s=feedStock[t];if(s.qty<=s.threshold)alerts.push('🌾 Feed <strong>'+esc(t)+'</strong>: only '+s.qty+' sacks remaining');});
  Object.keys(medStock).forEach(function(m){var s=medStock[m];if(s.qty<=s.threshold)alerts.push('💊 Medicine <strong>'+esc(m)+'</strong>: only '+s.qty+' units remaining');});
  // Brada stock tracking removed
  $('dash-alerts').innerHTML=alerts.length?alerts.map(function(a){return '<div style="padding:8px 0;border-bottom:1px solid var(--sand);font-size:0.82rem">⚠️ '+a+'</div>';}).join(''):
    '<div class="empty"><div class="ei">✅</div><div class="et">All stocks healthy.</div></div>';
}

// ════════════════════════════════════════════════════
//  FLOCK CARDS
// ════════════════════════════════════════════════════
function renderFlocks(){
  var container=$('flock-list');
  if(!flocks.length){container.innerHTML='<div class="empty" style="background:var(--white);border:2px solid var(--border);border-radius:12px;padding:50px 20px"><div class="ei">🐣</div><div class="et">No flocks yet.</div></div>';return;}
  container.innerHTML='';
  flocks.forEach(function(f){
    var isActive=f.status==='Active';
    var card=document.createElement('div');card.className='fc';
    card.innerHTML='<div class="fc-head"><span class="fc-id">'+esc(f.id)+'</span>'
      +'<div class="fc-info"><div class="fc-breed">'+esc(f.breed)+'</div>'
      +'<div class="fc-meta">'+f.qty.toLocaleString()+' birds · Arrived '+fmt(f.arrivalDate)+' · '+esc(f.supplierName)+'</div></div>'
      +'<div class="fc-badge"><span class="badge '+(isActive?'b-active':'b-closed')+'">'+f.status+'</span></div></div>'
      +(isActive
        ?'<div class="fc-actions"><span class="fc-actions-label">Actions</span>'
          +'<button class="btn btn-outline btn-sm fc-btn-view" data-fid="'+f.id+'">🔍 View</button>'
          +'<button class="btn btn-primary btn-sm fc-btn-edit" data-fid="'+f.id+'">✏️ Update</button>'
          +'<button class="btn btn-red btn-sm fc-btn-close" data-fid="'+f.id+'">✕ Close Flock</button></div>'
        :'<div class="fc-closed-bar"><span class="fc-locked-icon">🔒</span>Closed on <strong>'+fmt(f.closeDate)+'</strong>'
          +'&nbsp;&nbsp;<button class="btn btn-outline btn-sm fc-btn-view" data-fid="'+f.id+'" style="margin-left:auto">View Details</button></div>');
    container.appendChild(card);
  });
  container.querySelectorAll('.fc-btn-view').forEach(function(b){b.addEventListener('click',function(){openViewModal(b.getAttribute('data-fid'));});});
  container.querySelectorAll('.fc-btn-edit').forEach(function(b){b.addEventListener('click',function(){openEditModal(b.getAttribute('data-fid'));});});
  container.querySelectorAll('.fc-btn-close').forEach(function(b){b.addEventListener('click',function(){openCloseModal(b.getAttribute('data-fid'));});});
}

// ════════════════════════════════════════════════════
//  US-001: REGISTER FLOCK
// ════════════════════════════════════════════════════
$('btn-reg').addEventListener('click',function(){
  ['fg-rb','fg-rq','fg-rd','fg-rs'].forEach(function(id){inv(id,false);});
  $('r-breed').value='';$('r-qty').value='';$('r-date').value=today();$('r-notes').value='';
  fillSupSelect('r-sup',['Chicks Supplier']);openM('m-reg');
});
$('do-reg').addEventListener('click',async function(){
  var breed=$('r-breed').value.trim(),qty=parseInt($('r-qty').value,10),date=$('r-date').value,supId=$('r-sup').value,notes=$('r-notes').value.trim();
  var ok=true;
  inv('fg-rb',!breed);if(!breed)ok=false;
  inv('fg-rq',!qty||qty<1);if(!qty||qty<1)ok=false;
  inv('fg-rd',!date);if(!date)ok=false;
  inv('fg-rs',!supId);if(!supId)ok=false;
  if(!ok)return;
  var sup=suppliers.find(function(s){return String(s.id)===String(supId);});
  try{
    var createRes = await api('/flocks',{
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body:JSON.stringify({
        breed:breed,
        initialQty:qty,
        arrivalDate:date,
        supplierId:supId,
        notes:notes,
        createdBy:null
      })
    });
    if(!createRes.ok){
      toast('Failed to register flock in backend.','t-bad');
      return;
    }
    var createdFlock = await createRes.json();
    await loadFlocksFromApi();
    var mapped = mapFlockFromApi(createdFlock);
    addLog(mapped.id,'Registered','Breed: '+breed+', Qty: '+qty+', Supplier: '+(sup ? sup.name : 'Unknown'));
    closeM('m-reg');toast('Flock '+mapped.id+' registered!','t-ok');
  }catch(_err){
    toast('Backend not reachable. Could not register flock.','t-bad');
    return;
  }
  renderFlocks();renderDash();
});

// ════════════════════════════════════════════════════
//  US-002: UPDATE FLOCK
// ════════════════════════════════════════════════════
function openEditModal(fid){
  var f=flocks.find(function(x){return x.id===fid;});if(!f)return;
  editTarget=fid;var isClosed=f.status==='Closed';
  $('edit-title').textContent=isClosed?'Flock Details (Read-Only)':'Update Flock Information';
  $('edit-sub').textContent=fid+(isClosed?' — Closed '+fmt(f.closeDate):' — Active');
  $('edit-ro').style.display=isClosed?'':'none';
  $('edit-active-hint').style.display=isClosed?'none':'';
  $('e-id').value=f.id;$('e-status').value=f.status;$('e-breed').value=f.breed;
  $('e-qty').value=f.qty;$('e-date').value=f.arrivalDate;$('e-supplier').value=f.supplierName;$('e-notes').value=f.notes||'';
  ['e-breed','e-qty','e-notes'].forEach(function(id){$(id).readOnly=isClosed;$(id).disabled=isClosed;});
  $('do-edit').style.display=isClosed?'none':'';$('edit-nochange').style.display='none';
  ['fg-eb','fg-eq'].forEach(function(id){inv(id,false);});openM('m-edit');
}
$('do-edit').addEventListener('click',async function(){
  var breed=$('e-breed').value.trim(),qty=parseInt($('e-qty').value,10),notes=$('e-notes').value.trim(),ok=true;
  inv('fg-eb',!breed);if(!breed)ok=false;
  inv('fg-eq',!qty||qty<1);if(!qty||qty<1)ok=false;
  if(!ok)return;
  var f=flocks.find(function(x){return x.id===editTarget;});
  var diff=[];
  if(f.breed!==breed)diff.push({field:'Breed',old:f.breed,nw:breed});
  if(f.qty!==qty)diff.push({field:'Quantity',old:f.qty,nw:qty});
  if((f.notes||'')!==notes)diff.push({field:'Notes',old:f.notes||'(empty)',nw:notes||'(empty)'});
  if(!diff.length){$('edit-nochange').style.display='';return;}
  $('edit-nochange').style.display='none';
  try{
    var updateRes = await api('/flocks/'+encodeURIComponent(f.flockDbId || ''),{
      method:'PUT',
      headers:{'Content-Type':'application/json'},
      body:JSON.stringify({
        breed:breed,
        currentQty:qty,
        notes:notes,
        updatedBy:null
      })
    });
    if(!updateRes.ok){
      toast('Failed to update flock in backend.','t-bad');
      return;
    }
    var updated = await updateRes.json();
    var mapped = mapFlockFromApi(updated);
    flocks = flocks.map(function(f0){ return String(f0.id) === String(editTarget) ? mapped : f0; });
  }catch(_err){
    toast('Backend not reachable. Could not update flock.','t-bad');
    return;
  }
  addLog(editTarget,'Updated',diff.map(function(d){return d.field;}).join(', ')+' updated',diff);
  closeM('m-edit');toast('Flock '+editTarget+' updated.','t-ok');renderFlocks();renderDash();editTarget=null;
});

// ════════════════════════════════════════════════════
//  US-003: CLOSE FLOCK
// ════════════════════════════════════════════════════
function openCloseModal(fid){
  var f=flocks.find(function(x){return x.id===fid;});
  if(!f||f.status!=='Active'){toast('Already closed.','t-bad');return;}
  closeTarget=fid;$('close-fid-lbl').textContent=fid;$('close-date').value=today();
  $('close-unsold').style.display=f.qty>0?'':'none';
  $('close-chk').checked=false;$('do-close').disabled=true;inv('fg-cd',false);openM('m-close');
}
$('close-chk').addEventListener('change',function(){$('do-close').disabled=!this.checked;});
$('do-close').addEventListener('click',async function(){
  var cd=$('close-date').value;inv('fg-cd',!cd);if(!cd)return;
  var f=flocks.find(function(x){return x.id===closeTarget;});
  try{
    var closeRes = await api('/flocks/'+encodeURIComponent(f.flockDbId || '')+'/close',{
      method:'PATCH',
      headers:{'Content-Type':'application/json'},
      body:JSON.stringify({closeDate:cd,closedBy:EDITOR})
    });
    if(!closeRes.ok){
      toast('Failed to close flock in backend.','t-bad');
      return;
    }
    var closed = await closeRes.json();
    var mapped = mapFlockFromApi(closed);
    flocks = flocks.map(function(f0){ return String(f0.id) === String(closeTarget) ? mapped : f0; });
  }catch(_err){
    toast('Backend not reachable. Could not close flock.','t-bad');
    return;
  }
  addLog(closeTarget,'Closed','Close date: '+fmt(cd),[{field:'Status',old:'Active',nw:'Closed'},{field:'Close Date',old:'—',nw:fmt(cd)}]);
  closeM('m-close');toast('Flock '+closeTarget+' closed.','t-ok');renderFlocks();renderDash();closeTarget=null;
});

// ════════════════════════════════════════════════════
//  VIEW FLOCK MODAL
// ════════════════════════════════════════════════════
function openViewModal(fid){
  var f=flocks.find(function(x){return x.id===fid;});if(!f)return;
  viewTarget=fid;var isActive=f.status==='Active';
  $('view-title').textContent='Flock '+f.id;$('view-sub').textContent=f.breed+' · '+f.qty.toLocaleString()+' birds';
  $('view-ro').style.display=isActive?'none':'';
  var extra='';
  if(f.closeDate)extra+='<div class="dg-i"><div class="dg-lbl">Close Date</div><div class="dg-val">'+fmt(f.closeDate)+'</div></div>';
  if(f.notes)extra+='<div class="dg-i" style="grid-column:1/-1"><div class="dg-lbl">Notes</div><div class="dg-val" style="font-weight:400">'+esc(f.notes)+'</div></div>';
  $('view-dg').innerHTML='<div class="dg-i"><div class="dg-lbl">Flock ID</div><div class="dg-val" style="color:var(--accent);font-family:\'DM Mono\',monospace">'+f.id+'</div></div>'
    +'<div class="dg-i"><div class="dg-lbl">Status</div><div class="dg-val"><span class="badge '+(isActive?'b-active':'b-closed')+'">'+f.status+'</span></div></div>'
    +'<div class="dg-i"><div class="dg-lbl">Breed</div><div class="dg-val">'+esc(f.breed)+'</div></div>'
    +'<div class="dg-i"><div class="dg-lbl">Current Quantity</div><div class="dg-val">'+f.qty.toLocaleString()+'</div></div>'
    +'<div class="dg-i"><div class="dg-lbl">Original Quantity</div><div class="dg-val">'+f.origQty.toLocaleString()+'</div></div>'
    +'<div class="dg-i"><div class="dg-lbl">Arrival Date</div><div class="dg-val">'+fmt(f.arrivalDate)+'</div></div>'
    +'<div class="dg-i"><div class="dg-lbl">Supplier</div><div class="dg-val">'+esc(f.supplierName)+'</div></div>'+extra;
  var flog=auditLog.filter(function(e){return e.flockUi===fid;});
  $('view-log').innerHTML=buildLogHtml(flog);
  // Daily records tab
  var fm=mortalities.filter(function(m){return m.flockId===fid;});
  var fw=weeklyRecords.filter(function(w){return w.flockId===fid;});
  $('view-daily-content').innerHTML='<p style="font-weight:600;margin-bottom:8px">Mortality ('+fm.length+' records)</p>'
    +(fm.length?'<table><thead><tr><th>Date</th><th>Deaths</th><th>Notes</th></tr></thead><tbody>'
      +fm.map(function(m){return '<tr><td>'+fmt(m.date)+'</td><td>'+m.count+'</td><td>'+esc(m.notes||'—')+'</td></tr>';}).join('')
      +'</tbody></table>':'<div class="empty" style="padding:20px"><div class="et">No mortality records.</div></div>')
    +'<p style="font-weight:600;margin:16px 0 8px">Weekly Records ('+fw.length+')</p>'
    +(fw.length?'<table><thead><tr><th>Date</th><th>Age (days)</th><th>Remaining</th><th>Feed (kg)</th><th>Avg Weight (g)</th><th>FCR</th></tr></thead><tbody>'
      +fw.map(function(w){return '<tr><td>'+fmt(w.date)+'</td><td>'+(w.ageDays||'—')+'</td><td>'+(w.remainingChicks||'—')+'</td><td>'+(w.feedUsedKg||0)+'</td><td>'+w.avgWeightG+'</td><td>'+(w.fcr!==null&&w.fcr!==undefined?Number(w.fcr).toFixed(3):'—')+'</td></tr>';}).join('')
      +'</tbody></table>':'<div class="empty" style="padding:20px"><div class="et">No weekly records.</div></div>');
  // Sales tab
  var fs=flockSales.filter(function(s){return s.flockId===fid;});
  $('view-sales-content').innerHTML=(fs.length?'<table><thead><tr><th>Date</th><th>Buyer</th><th>Net Weight (kg)</th><th>Rate/kg</th><th>Total</th></tr></thead><tbody>'
    +fs.map(function(s){return '<tr><td>'+fmt(s.date)+'</td><td>'+esc(s.buyer)+'</td><td>'+s.netW+'</td><td>₨'+s.rate+'</td><td><strong>'+rupees(s.total)+'</strong></td></tr>';}).join('')
    +'</tbody></table>':'<div class="empty" style="padding:20px"><div class="et">No sales recorded for this flock.</div></div>');
  $('view-to-edit').style.display=isActive?'':'none';$('view-to-close').style.display=isActive?'':'none';
  document.querySelectorAll('.mtab').forEach(function(t){t.classList.remove('on');});
  document.querySelectorAll('.mpane').forEach(function(p){p.classList.remove('on');});
  document.querySelector('[data-tab="vov"]').classList.add('on');$('pane-vov').classList.add('on');
  openM('m-view');
}
document.querySelectorAll('.mtab').forEach(function(btn){
  btn.addEventListener('click',function(){
    document.querySelectorAll('.mtab').forEach(function(t){t.classList.remove('on');});
    document.querySelectorAll('.mpane').forEach(function(p){p.classList.remove('on');});
    btn.classList.add('on');$('pane-'+btn.getAttribute('data-tab')).classList.add('on');
  });
});
$('view-to-edit').addEventListener('click',function(){var fid=viewTarget;closeM('m-view');openEditModal(fid);});
$('view-to-close').addEventListener('click',function(){var fid=viewTarget;closeM('m-view');openCloseModal(fid);});

// ════════════════════════════════════════════════════
//  US-004: RECORD MORTALITY
// ════════════════════════════════════════════════════
$('btn-mort').addEventListener('click',function(){
  fillFlockSelect('mort-flock');
  $('mort-date').value=today();
  $('mort-day').value='';$('mort-night').value='';
  $('mort-type').value='';
  $('mort-24').textContent='—';
  $('mort-notes').value='';$('mort-hint').textContent='';
  ['fg-mort-f','fg-mort-d','fg-mort-day','fg-mort-night','fg-mort-type'].forEach(function(id){inv(id,false);});
  openM('m-mort');
});
$('mort-flock').addEventListener('change',function(){
  var fid=this.value,f=fid&&flocks.find(function(x){return x.id===fid;});
  $('mort-hint').textContent=f?'Current live birds: '+f.qty:'';
});
function calcMort24(){
  var d=parseInt($('mort-day').value,10);if(isNaN(d)||d<0)d=0;
  var n=parseInt($('mort-night').value,10);if(isNaN(n)||n<0)n=0;
  $('mort-24').textContent=String(d+n);
}
['mort-day','mort-night'].forEach(function(id){$(id).addEventListener('input',calcMort24);});
$('do-mort').addEventListener('click',async function(){
  var flockId=$('mort-flock').value,date=$('mort-date').value;
  var day=parseInt($('mort-day').value,10),night=parseInt($('mort-night').value,10);
  var mtype=$('mort-type').value;
  var notes=$('mort-notes').value.trim();
  if(isNaN(day)||day<0)day=0;
  if(isNaN(night)||night<0)night=0;
  var count=day+night;
  var ok=true;
  inv('fg-mort-f',!flockId);if(!flockId)ok=false;
  inv('fg-mort-d',!date);if(!date)ok=false;
  inv('fg-mort-day',isNaN(day)||day<0);if(isNaN(day)||day<0)ok=false;
  inv('fg-mort-night',isNaN(night)||night<0);if(isNaN(night)||night<0)ok=false;
  inv('fg-mort-type',!mtype);if(!mtype)ok=false;
  if(!ok)return;
  var f=flocks.find(function(x){return x.id===flockId;});
  if(count>f.qty){toast('Count exceeds remaining birds!','t-bad');return;}
  var fidDb=flockDbUuid(flockId);
  if(!fidDb){toast('Flock is not linked to backend (missing id).','t-bad');return;}
  if(count<1){toast('Enter day and/or night mortality.','t-bad');return;}
  var detailNote=(notes?notes+' · ':'')+mtype;
  try{
    if(day>0){
      var r1=await api('/mortality',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({flockId:fidDb,recordDate:date,count:day,shift:'DAY',type:mtype})});
      if(!r1.ok){var t1=await r1.text().catch(function(){return '';});toast('Mortality (day) failed: '+(t1||r1.status),'t-bad');return;}
    }
    if(night>0){
      var r2=await api('/mortality',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({flockId:fidDb,recordDate:date,count:night,shift:'NIGHT',type:mtype})});
      if(!r2.ok){var t2=await r2.text().catch(function(){return '';});toast('Mortality (night) failed: '+(t2||r2.status),'t-bad');return;}
    }
  }catch(_e){
    toast('Backend not reachable for mortality.','t-bad');
    return;
  }
  await loadMortalityFromApi();
  await loadFlocksFromApi();
  var cumMort=totalMortalityForFlock(flockId);
  addLog(flockId,'Mortality Recorded',count+' deaths ('+mtype+') on '+fmt(date)+'. Cumulative: '+cumMort+(detailNote?'. '+detailNote:''));
  closeM('m-mort');toast('Mortality recorded.','t-ok');renderDaily();renderFlocks();renderDash();
});

// ════════════════════════════════════════════════════
//  US-005: WEEKLY RECORD (WEIGHT + FCR)
// ════════════════════════════════════════════════════
function updateWeeklyModalCalcs(){
  var flockId=$('wt-flock').value;
  var date=$('wt-date').value;
  if(!flockId||!date){$('wt-age').textContent='—';$('wt-total').textContent='—';$('wt-mort').textContent='—';$('wt-rem').textContent='—';$('wt-feedkg').textContent='—';$('wt-fcr').textContent='—';return;}
  var age=chickAgeDays(flockId,date);
  var remaining=remainingChicksForFlock(flockId);
  var f=flocks.find(function(x){return x.id===flockId;});
  var totalChicks=f?(f.origQty||0):0;
  var totalMort=totalMortalityForFlock(flockId);
  $('wt-age').textContent=age!==null?String(age):'—';
  $('wt-total').textContent=String(totalChicks);
  $('wt-mort').textContent=String(totalMort);
  $('wt-rem').textContent=remaining!==null?String(remaining):'—';
  var end=date;
  var startDateObj=new Date(date+'T00:00:00');startDateObj.setDate(startDateObj.getDate()-6);
  var start=startDateObj.getFullYear()+'-'+pad(startDateObj.getMonth()+1)+'-'+pad(startDateObj.getDate());
  var feedKg=feedUsedKgForFlockBetween(flockId,start,end);
  $('wt-feedkg').textContent=feedKg?String(feedKg):'—';
  var avgG=parseFloat($('wt-weight').value)||0;
  var liveKg=remaining!==null?(remaining*(avgG/1000)):0;
  var fcr=(feedKg>0&&liveKg>0)?(liveKg/feedKg):null; // per spec: TotalLiveWeight / TotalFeedUsed
  $('wt-fcr').textContent=fcr!==null?fcr.toFixed(3):'—';
}
$('btn-weekly').addEventListener('click',function(){
  fillFlockSelect('wt-flock');
  $('wt-date').value=today();
  $('wt-weight').value='';
  $('wt-notes').value='';
  updateWeeklyModalCalcs();
  ['fg-wt-f','fg-wt-d','fg-wt-w'].forEach(function(id){inv(id,false);});
  openM('m-weight');
});
['wt-flock','wt-date','wt-weight'].forEach(function(id){$(id).addEventListener('change',updateWeeklyModalCalcs);$(id).addEventListener('input',updateWeeklyModalCalcs);});
$('do-weight').addEventListener('click',async function(){
  var flockId=$('wt-flock').value,date=$('wt-date').value,avgG=parseFloat($('wt-weight').value),notes=$('wt-notes').value.trim();
  var ok=true;
  inv('fg-wt-f',!flockId);if(!flockId)ok=false;
  inv('fg-wt-d',!date);if(!date)ok=false;
  inv('fg-wt-w',!avgG||avgG<1);if(!avgG||avgG<1)ok=false;
  if(!ok)return;
  if(avgG>10000)if(!confirm('Weight of '+avgG+'g seems very high. Continue?'))return;
  var fidDb=flockDbUuid(flockId);
  if(!fidDb){toast('Flock is not linked to backend (missing id).','t-bad');return;}
  var wk=weekNumberForFlockDate(flockId,date);
  var avgKg=avgG/1000;
  try{
    var wr=await api('/weights',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({flockId:fidDb,weekNumber:wk,recordDate:date,avgWeightKg:avgKg})});
    if(!wr.ok){var wt=await wr.text().catch(function(){return '';});toast('Weekly weight failed: '+(wt||wr.status),'t-bad');return;}
  }catch(_e){
    toast('Backend not reachable for weekly weight.','t-bad');
    return;
  }
  var startDateObj=new Date(date+'T00:00:00');startDateObj.setDate(startDateObj.getDate()-6);
  var start=startDateObj.getFullYear()+'-'+pad(startDateObj.getMonth()+1)+'-'+pad(startDateObj.getDate());
  var weeklySacks=feedSacksForFlockBetween(flockId,start,date);
  var cumSacks=cumulativeFeedSacksThrough(flockId,date);
  try{
    var sr=await api('/weekly-summary',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({
      flockId:fidDb,
      weekNumber:wk,
      weekEndDate:date,
      weeklyFeedSacks:weeklySacks,
      cumulativeFeedSacks:cumSacks
    })});
    if(!sr.ok){ /* snapshot optional */ }
  }catch(_e){ /* ignore */ }
  await loadWeightsFromApi();
  await loadFlocksFromApi();
  var age=chickAgeDays(flockId,date);
  var totalChicks=(flocks.find(function(f){return f.id===flockId;})||{}).origQty||0;
  var totalMort=totalMortalityForFlock(flockId);
  var remaining=Math.max(0,totalChicks-totalMort);
  var feedKg=feedUsedKgForFlockBetween(flockId,start,date);
  var liveKg=remaining*(avgG/1000);
  var fcr=(feedKg>0&&liveKg>0)?(liveKg/feedKg):null;
  addLog(flockId,'Weekly Record',fmt(date)+': '+avgG+'g, feed '+feedKg+'kg, FCR '+(fcr!==null?fcr.toFixed(3):'—')+(notes?'. '+notes:''));
  closeM('m-weight');toast('Weekly record saved.','t-ok');renderDaily();renderDash();
});

function renderDaily(){
  var dtab=document.querySelector('#daily-tabs .tab-pill.on[data-dtab]');
  var active=dtab?dtab.getAttribute('data-dtab'):'mortality';
  renderMortTable();renderDailyRecordTable();renderWeightTable();
  $('daily-mortality-pane').style.display=active==='mortality'?'':'none';
  $('daily-daily-pane').style.display=active==='daily-record'?'':'none';
  $('daily-weight-pane').style.display=active==='weekly'?'':'none';
}
function renderDailyRecordTable(){
  var tb=$('daily-record-tbody');
  if(!tb)return;
  var entries=[];
  feedTxns.filter(function(t){return t.type==='Usage';}).forEach(function(t){
    var q=Number(t.qty||0);
    entries.push({
      date:t.date||'',
      flockId:t.flockId||'—',
      type:'Feed',
      item:t.feedType||'—',
      qtyLabel:q+' sack'+(q===1?'':'s'),
      meta:esc(t.shift||'—')
    });
  });
  medTxns.filter(function(t){return t.type==='Usage';}).forEach(function(t){
    var parts=[];
    if(t.usageTime)parts.push(t.usageTime);
    if(t.notes)parts.push(t.notes);
    entries.push({
      date:t.date||'',
      flockId:t.flockId||'—',
      type:'Medicine',
      item:t.medicine||'—',
      qtyLabel:String(t.qty!=null?t.qty:'')+(t.unit?(' '+t.unit):''),
      meta:esc(parts.length?parts.join(' · '):'—')
    });
  });
  entries.sort(function(a,b){
    var c=String(b.date).localeCompare(String(a.date));
    if(c!==0)return c;
    return String(a.type).localeCompare(String(b.type));
  });
  tb.innerHTML=entries.length?entries.map(function(e){
    return '<tr><td>'+fmt(e.date)+'</td><td><span class="fc-id" style="font-size:0.75rem">'+esc(e.flockId)+'</span></td>'
      +'<td><span class="badge '+(e.type==='Feed'?'b-active':'b-processed')+'">'+esc(e.type)+'</span></td>'
      +'<td>'+esc(e.item)+'</td><td style="font-weight:600">'+esc(e.qtyLabel)+'</td><td style="color:var(--muted);font-size:0.85rem">'+e.meta+'</td></tr>';
  }).join(''):'<tr><td colspan="6"><div class="empty" style="padding:20px"><div class="et">No feed or medicine usage yet. Use Record Feed or Record Medicine above.</div></div></td></tr>';
}
function renderMortTable(){
  // allow multiple Hospital/Shed entries per day by grouping display by (date, flock)
  var rows=mortalities.slice().sort(function(a,b){return b.date.localeCompare(a.date);});
  var grouped={};var order=[];
  rows.forEach(function(m){
    var k=m.flockId+'|'+m.date;
    if(!grouped[k]){grouped[k]={date:m.date,flockId:m.flockId,total:0,cumulative:m.cumulative||0,editor:m.editor,parts:[]};order.push(k);}
    grouped[k].total+=m.count||0;
    grouped[k].cumulative=Math.max(grouped[k].cumulative,(m.cumulative||0));
    grouped[k].parts.push({type:m.type||'—',day:m.day||0,night:m.night||0,count:m.count||0});
  });
  $('mort-tbody').innerHTML=order.length?order.map(function(k){
    var g=grouped[k];
    var f=flocks.find(function(x){return x.id===g.flockId;});
    var remaining=f?Math.max(0,(f.origQty||0)-(g.cumulative||0)):null;
    var detail=g.parts.map(function(p){return esc(p.type)+': D'+p.day+' N'+p.night+' (T'+p.count+')';}).join(' · ');
    return '<tr><td>'+fmt(g.date)+'</td><td><span class="fc-id" style="font-size:0.75rem">'+esc(g.flockId)+'</span></td><td style="color:var(--red);font-weight:700">'+g.total+'</td><td>'+g.cumulative+'</td><td>'+(remaining!==null?remaining.toLocaleString():'—')+'</td><td style="color:var(--muted)">'+esc(g.editor)+'</td></tr>'
      +'<tr><td colspan="6" style="padding-top:0"><div style="font-size:0.72rem;color:var(--muted);padding:0 12px 10px">'+detail+'</div></td></tr>';
  }).join(''):'<tr><td colspan="6"><div class="empty" style="padding:20px"><div class="et">No mortality records yet.</div></div></td></tr>';
}
function renderWeightTable(){
  var rows=weeklyRecords.slice().sort(function(a,b){return b.date.localeCompare(a.date);});
  $('weight-tbody').innerHTML=rows.length?rows.map(function(r){
    var fcr=r.fcr!==null&&r.fcr!==undefined?Number(r.fcr).toFixed(3):'—';
    return '<tr><td>'+fmt(r.date)+'</td><td><span class="fc-id" style="font-size:0.75rem">'+esc(r.flockId)+'</span></td>'
      +'<td>'+(r.ageDays||'—')+'</td><td>'+(r.totalChicks||0)+'</td><td>'+(r.totalMortality||0)+'</td><td>'+(r.remainingChicks!==null?r.remainingChicks:'—')+'</td><td>'+(r.feedUsedKg||0)+'</td><td style="font-weight:600">'+r.avgWeightG+'g</td><td>'+fcr+'</td></tr>';
  }).join(''):'<tr><td colspan="9"><div class="empty" style="padding:20px"><div class="et">No weekly records yet.</div></div></td></tr>';
}
document.querySelectorAll('#daily-tabs [data-dtab]').forEach(function(btn){
  btn.addEventListener('click',function(){
    document.querySelectorAll('#daily-tabs [data-dtab]').forEach(function(b){b.classList.remove('on');});
    btn.classList.add('on');renderDaily();
  });
});

// ════════════════════════════════════════════════════
//  US-006: FLOCK SALE
// ════════════════════════════════════════════════════
$('btn-flock-sale').addEventListener('click',function(){
  fillFlockSelect('fs-flock');
  $('fs-date').value=today();$('fs-buyer').value='';
  $('fs-qtykg').value='';
  $('fs-veh').value='';$('fs-empty').value='';$('fs-loaded').value='';
  $('fs-rate').value='';$('fs-less').value='';$('fs-com').value='';
  $('fs-net').textContent='—';$('fs-gross').textContent='₨ —';$('fs-final').textContent='₨ —';
  $('fs-notes').value='';$('fs-hint').textContent='';
  ['fg-fs-f','fg-fs-d','fg-fs-b','fg-fs-q','fg-fs-veh','fg-fs-ew','fg-fs-lw','fg-fs-r'].forEach(function(id){inv(id,false);});
  openM('m-fsale');
});
function calcFsAmounts(){
  var empty=parseFloat($('fs-empty').value)||0;
  var loaded=parseFloat($('fs-loaded').value)||0;
  var rate=parseFloat($('fs-rate').value)||0;
  var less=parseFloat($('fs-less').value)||0;
  var com=parseFloat($('fs-com').value)||0;
  var net=Math.max(0,loaded-empty);
  var gross=net*rate;
  var finalAmt=Math.max(0,gross-less-com);
  $('fs-net').textContent=net?String(net):'—';
  $('fs-gross').textContent=gross>0?rupees(gross):'₨ —';
  $('fs-final').textContent=(gross||less||com)?rupees(finalAmt):'₨ —';
}
['fs-empty','fs-loaded','fs-rate','fs-less','fs-com'].forEach(function(id){$(id).addEventListener('input',calcFsAmounts);});
$('fs-flock').addEventListener('change',function(){
  var f=this.value&&flocks.find(function(x){return x.id===$('fs-flock').value;});
  $('fs-hint').textContent=f?'Remaining birds (after mortality): '+f.qty:'';
});
$('do-fsale').addEventListener('click',async function(){
  var flockId=$('fs-flock').value,date=$('fs-date').value,buyer=$('fs-buyer').value.trim();
  var qtyKg=parseFloat($('fs-qtykg').value);
  var vehicle=$('fs-veh').value.trim();
  var emptyW=parseFloat($('fs-empty').value),loadedW=parseFloat($('fs-loaded').value);
  var rate=parseFloat($('fs-rate').value);
  var less=parseFloat($('fs-less').value)||0,commission=parseFloat($('fs-com').value)||0;
  var ok=true;
  inv('fg-fs-f',!flockId);if(!flockId)ok=false;inv('fg-fs-d',!date);if(!date)ok=false;
  inv('fg-fs-b',!buyer);if(!buyer)ok=false;
  inv('fg-fs-q',!qtyKg||qtyKg<=0);if(!qtyKg||qtyKg<=0)ok=false;
  inv('fg-fs-veh',!vehicle);if(!vehicle)ok=false;
  inv('fg-fs-ew',!emptyW||emptyW<0);if(!emptyW&&emptyW!==0)ok=false;
  inv('fg-fs-lw',!loadedW||loadedW<0);if(!loadedW&&loadedW!==0)ok=false;
  inv('fg-fs-r',!rate||rate<=0);if(!rate||rate<=0)ok=false;
  if(!ok)return;
  if(loadedW<emptyW){toast('Loaded weight must be ≥ empty weight.','t-bad');return;}
  var netW=Math.max(0,loadedW-emptyW);
  var gross=netW*rate;
  var total=Math.max(0,gross-less-commission);
  var fidDb=flockDbUuid(flockId);
  if(!fidDb){toast('Flock is not linked to backend.','t-bad');return;}
  try{
    var fr=await api('/sales/flock',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({
      flockId:fidDb,
      saleDate:date,
      buyerName:buyer,
      qtySold:1,
      weightPerBirdKg:netW,
      pricePerKg:rate
    })});
    if(!fr.ok){var ft=await fr.text().catch(function(){return '';});toast('Flock sale failed: '+(ft||fr.status),'t-bad');return;}
    await loadFlockSalesFromApi();
    await loadFlocksFromApi();
  }catch(_e){
    toast('Backend not reachable for flock sale.','t-bad');
    return;
  }
  addLog(flockId,'Flock Sale','Net '+netW+'kg to '+buyer+' for '+rupees(total));
  closeM('m-fsale');toast('Sale recorded: '+rupees(total),'t-ok');renderSalesView();renderFlocks();renderDash();
});

// ════════════════════════════════════════════════════
//  US-022: OTHER SALES
// ════════════════════════════════════════════════════
$('btn-other-sale').addEventListener('click',function(){
  $('os-date').value=today();$('os-cat').value='';$('os-desc').value='';$('os-buyer').value='';$('os-amount').value='';
  ['fg-os-d','fg-os-cat','fg-os-desc','fg-os-amt'].forEach(function(id){inv(id,false);});openM('m-osale');
});
$('do-osale').addEventListener('click',async function(){
  var date=$('os-date').value,cat=$('os-cat').value,desc=$('os-desc').value.trim(),amount=parseFloat($('os-amount').value),ok=true;
  inv('fg-os-d',!date);if(!date)ok=false;inv('fg-os-cat',!cat);if(!cat)ok=false;
  inv('fg-os-desc',!desc);if(!desc)ok=false;inv('fg-os-amt',!amount||amount<=0);if(!amount||amount<=0)ok=false;
  if(!ok)return;
  var buyerStr=$('os-buyer').value.trim()||null;
  var payloadNew={
    saleDate:date,
    category:uiOtherSaleCategoryToApi(cat),
    description:desc,
    buyerName:buyerStr,
    amount:amount
  };
  var payloadLegacy={
    saleDate:date,
    category:uiOtherSaleCategoryToApi(cat),
    description:desc,
    buyer:buyerStr,
    amount:amount
  };
  try{
    var createRes=await api('/sales/other',{
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body:JSON.stringify(payloadNew)
    });
    if(!createRes.ok){
      createRes=await api('/other-sales',{
        method:'POST',
        headers:{'Content-Type':'application/json'},
        body:JSON.stringify(payloadLegacy)
      });
    }
    if(!createRes.ok){toast('Failed to save other sale in backend.','t-bad');return;}
    await loadOtherSalesFromApi();
  }catch(_err){
    toast('Backend not reachable. Could not save other sale.','t-bad');
    return;
  }
  addLog('Other Sale','Added',desc+' — '+rupees(amount));
  closeM('m-osale');toast('Sale recorded.','t-ok');renderSalesView();renderDash();
});

function renderSalesView(){
  var allTxns=[];
  flockSales.forEach(function(s){allTxns.push({date:s.date,type:'Flock Sale',ref:s.flockId,detail:s.buyer+' | Net '+s.netW+'kg @ ₨'+s.rate+'/kg',amount:s.total});});
  otherSales.forEach(function(s){allTxns.push({date:s.date,type:s.category,ref:s.desc,detail:s.buyer||'—',amount:s.amount});});
  var sacksSales=feedTxns.filter(function(t){ return t.type==='Sale'; });
  sacksSales.forEach(function(s){
    allTxns.push({
      date:s.date,
      type:'Sacks Sale',
      ref:'Sacks Sale',
      detail:(s.buyer||'—')+' | '+Number(s.qty||0)+' sacks',
      amount:Number(s.cost||0)
    });
  });
  allTxns.sort(function(a,b){return b.date.localeCompare(a.date);});
  var totalFS=flockSales.reduce(function(s,x){return s+x.total;},0);
  var totalOS=otherSales.reduce(function(s,x){return s+x.amount;},0)
    +sacksSales.reduce(function(s,x){return s+Number(x.cost||0);},0);
  $('stat-fsale').textContent=rupees(totalFS);$('stat-osale').textContent=rupees(totalOS);
  $('stat-txns').textContent=allTxns.length;$('stat-totalrev').textContent=rupees(totalFS+totalOS);
  $('sales-tbody').innerHTML=allTxns.length?allTxns.map(function(t){
    return '<tr><td>'+fmt(t.date)+'</td><td><span class="badge b-active" style="background:var(--gl)">'+esc(t.type)+'</span></td><td>'+esc(t.ref)+'</td><td>'+esc(t.detail)+'</td><td>—</td><td style="font-weight:600;color:var(--green)">'+rupees(t.amount)+'</td></tr>';
  }).join(''):'<tr><td colspan="6"><div class="empty" style="padding:20px"><div class="et">No sales yet.</div></div></td></tr>';
}

// ════════════════════════════════════════════════════
//  US-007: SUPPLIERS
// ════════════════════════════════════════════════════
$('btn-add-sup').addEventListener('click',function(){
  supEditTarget=null;$('supplier-edit-id').value='';
  $('sup-modal-title').textContent='Add Supplier';
  $('s-name').value='';$('s-type').value='';$('s-contact').value='';$('s-address').value='';
  inv('fg-sn',false);$('fg-sdup').style.display='none';openM('m-sup');
});
$('do-sup').addEventListener('click',async function(){
  var name=$('s-name').value.trim();
  inv('fg-sn',!name);if(!name)return;
  var editId = ($('supplier-edit-id').value || '').trim();
  var dup=suppliers.find(function(s){
    return s.name.toLowerCase()===name.toLowerCase() && String(s.id) !== String(editId);
  });
  if(dup){$('fg-sdup').style.display='';return;}$('fg-sdup').style.display='none';
  var payload = {
    name:name,
    phone:$('s-contact').value.trim(),
    address:$('s-address').value.trim(),
    supplierType:uiTypeToSupplierTypeEnum($('s-type').value)
  };
  if(editId){
    var sup=suppliers.find(function(s){return String(s.id)===String(editId);});
    try{
      var updateRes = await api('/suppliers/'+encodeURIComponent(editId),{
        method:'PUT',
        headers:{'Content-Type':'application/json'},
        body:JSON.stringify(payload)
      });
      if(!updateRes.ok){
        var errText = updateRes.status === 409 ? 'Conflict: name may already exist.' : 'Failed to update supplier in backend.';
        toast(errText,'t-bad');
        return;
      }
      var updatedSupplier = await updateRes.json();
      sup = mapSupplierFromApi(updatedSupplier);
      suppliers = suppliers.map(function(s0){return String(s0.id)===String(editId)?sup:s0;});
    }catch(_err){
      toast('Backend not reachable. Could not update supplier.','t-bad');
      return;
    }
    addLog('Supplier','Edited','Updated: '+name);toast('Supplier updated.','t-ok');
  } else {
    try{
      var createRes = await api('/suppliers',{
        method:'POST',
        headers:{'Content-Type':'application/json'},
        body:JSON.stringify(payload)
      });
      if(!createRes.ok){
        toast(createRes.status === 409 ? 'A supplier with this name already exists.' : 'Failed to add supplier in backend.','t-bad');
        return;
      }
      var createdSupplier = await createRes.json();
      suppliers.push(mapSupplierFromApi(createdSupplier));
    }catch(_err){
      toast('Backend not reachable. Could not add supplier.','t-bad');
      return;
    }
    addLog('Supplier','Added','New supplier: '+name);toast('Supplier added.','t-ok');
  }
  closeM('m-sup');renderSuppliers();renderDash();
  if(activeSupId)renderSupHistoryPanel(activeSupId);
  supEditTarget=null;$('supplier-edit-id').value='';
});
var activeSupId = null;
var suphFilter  = 'all';
var supTypeFilter = 'all';

function supplierTypeForFilter(s){
  var t = (s && s.type) ? String(s.type).trim() : '';
  if(!t)return 'Other';
  return t;
}

function bindSupplierWorkerDelegation(){
  var supList = $('sup-list-inner');
  if(supList && !supList.__fcDelegation){
    supList.__fcDelegation = true;
    supList.addEventListener('click', function(e){
      var editBtn = e.target.closest('button.sup-edit-btn');
      if(editBtn){
        e.preventDefault();
        e.stopPropagation();
        var sid = editBtn.getAttribute('data-sid');
        if(sid) editSup(sid);
        return;
      }
      var row = e.target.closest('.sup-row');
      if(row){
        var sid = row.getAttribute('data-sid');
        if(sid) viewSupHistory(sid);
      }
    });
  }
  var wtBody = $('workers-tbody');
  if(wtBody && !wtBody.__fcDelegation){
    wtBody.__fcDelegation = true;
    wtBody.addEventListener('click', function(e){
      var editBtn = e.target.closest('button.worker-edit-btn');
      if(editBtn){
        e.preventDefault();
        var wid = editBtn.getAttribute('data-wid');
        if(wid) editWorker(wid);
      }
    });
  }
  var supFilterSel = $('sup-type-filter');
  if(supFilterSel && !supFilterSel.__fcBound){
    supFilterSel.__fcBound = true;
    supFilterSel.addEventListener('change', function(){
      supTypeFilter = supFilterSel.value || 'all';
      renderSuppliers();
    });
  }
}

function renderSuppliers(){
  var inner = $('sup-list-inner');
  if(!suppliers.length){
    inner.innerHTML = '<div class="empty" style="padding:30px"><div class="ei">🏭</div><div class="et">No suppliers yet.</div></div>';
    return;
  }
  var list = suppliers.filter(function(s){
    if(supTypeFilter === 'all') return true;
    return supplierTypeForFilter(s) === supTypeFilter;
  });
  if(!list.length){
    inner.innerHTML = '<div class="empty" style="padding:30px"><div class="ei">🔎</div><div class="et">No suppliers match this type.</div></div>';
    return;
  }
  inner.innerHTML = list.map(function(s){
    var txnCount = getSupTxns(s.id).length;
    var isActive = activeSupId === s.id;
    var sidAttr = esc(String(s.id));
    return '<div class="sup-row'+(isActive?' sup-row-active':'')+'" data-sid="'+sidAttr+'" style="display:flex;align-items:center;gap:12px;padding:12px 18px;border-bottom:1px solid var(--sand);cursor:pointer;transition:.12s;'+(isActive?'background:var(--al);border-left:3px solid var(--accent);':'')+'">'
      +'<div style="width:36px;height:36px;border-radius:9px;background:'+(isActive?'var(--accent)':'var(--sand2)')+';color:'+(isActive?'#fff':'var(--mid)')+';display:flex;align-items:center;justify-content:center;font-size:0.75rem;font-weight:700;flex-shrink:0">'+esc(s.name.charAt(0).toUpperCase())+'</div>'
      +'<div style="flex:1;min-width:0">'
        +'<div style="font-weight:600;font-size:0.88rem;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">'+esc(s.name)+'</div>'
        +'<div style="font-size:0.72rem;color:var(--muted);margin-top:1px">'+esc(s.type||'General')+' · '+(s.contact||'No contact')+'</div>'
      +'</div>'
      +'<div style="text-align:right;flex-shrink:0">'
        +'<div style="font-size:0.7rem;color:var(--muted)">'+txnCount+' txn'+(txnCount!==1?'s':'')+'</div>'
        +'<div style="display:flex;gap:5px;margin-top:4px">'
          +'<button type="button" class="btn btn-outline btn-sm sup-edit-btn" data-sid="'+sidAttr+'" style="padding:3px 8px;font-size:0.72rem" aria-label="Edit supplier">✏️</button>'
        +'</div>'
      +'</div>'
    +'</div>';
  }).join('');
}

function getSupTxns(sid){
  var txns=[];
  feedTxns.filter(function(t){return t.supId===sid;}).forEach(function(t){
    txns.push({date:t.date,mod:'Feed',icon:'🌾',detail:t.type+' — '+t.qty+' sacks of '+t.feedType,cost:t.cost||0,type:t.type});
  });
  medTxns.filter(function(t){return t.supId===sid;}).forEach(function(t){
    txns.push({date:t.date,mod:'Medicine',icon:'💊',detail:t.type+' — '+t.medicine+' × '+t.qty+' units',cost:t.cost||0,type:t.type});
  });
  bradaTxns.filter(function(t){return t.supId===sid&&t.type==='Purchase';}).forEach(function(t){
    txns.push({date:t.date,mod:'Brada',icon:'🪨',detail:'Purchase — '+t.qty+' bags',cost:t.cost||0,type:'Purchase'});
  });
  // Flocks registered from this supplier
  flocks.filter(function(f){return f.supId===sid;}).forEach(function(f){
    txns.push({date:f.arrivalDate,mod:'Flock',icon:'🐓',detail:'Flock registered — '+f.id+' ('+f.origQty+' birds, '+f.breed+')',cost:0,type:'Flock'});
  });
  txns.sort(function(a,b){return b.date.localeCompare(a.date);});
  return txns;
}

function editSup(sid){
  var s=suppliers.find(function(x){return String(x.id)===String(sid);});if(!s)return;
  supEditTarget=sid;
  $('supplier-edit-id').value=String(s.id);
  $('sup-modal-title').textContent='Edit Supplier';
  $('s-name').value=s.name;$('s-type').value=s.type||'';$('s-contact').value=s.contact||'';$('s-address').value=s.address||'';
  inv('fg-sn',false);$('fg-sdup').style.display='none';openM('m-sup');
}

// US-008: Inline Supplier History
function viewSupHistory(sid){
  var s=suppliers.find(function(x){return String(x.id)===String(sid);});if(!s)return;
  activeSupId=sid; suphFilter='all';
  renderSuppliers(); // re-render to highlight active row
  var panel=$('sup-history-panel');
  panel.style.display='';
  $('suph-inline-name').textContent=s.name;
  $('suph-inline-meta').textContent=(s.type||'General Supplier')+(s.contact?' · '+s.contact:'')+(s.address?' · '+s.address:'');
  renderSupHistoryPanel(sid);
}

function renderSupHistoryPanel(sid){
  var txns=getSupTxns(sid);
  var totalSpent=txns.reduce(function(sum,t){return sum+t.cost;},0);
  var lastOrder=txns.length?fmt(txns[0].date):'—';
  $('suph-txn-count').textContent=txns.length;
  $('suph-total-spent').textContent=rupees(totalSpent);
  $('suph-last-order').textContent=lastOrder;

  // Module filter pills
  var mods=['all','Feed','Medicine','Brada','Flock'];
  var modCounts={all:txns.length};
  txns.forEach(function(t){modCounts[t.mod]=(modCounts[t.mod]||0)+1;});
  $('suph-filter-pills').innerHTML=mods.filter(function(m){return m==='all'||modCounts[m];}).map(function(m){
    var label=m==='all'?'All ('+txns.length+')':({Feed:'🌾 Feed',Medicine:'💊 Medicine',Brada:'🪨 Brada',Flock:'🐓 Flocks'}[m]||m)+' ('+modCounts[m]+')';
    return '<button class="tab-pill'+(suphFilter===m?' on':'')+'" onclick="setSupFilter(\''+sid+'\',\''+m+'\')">'+label+'</button>';
  }).join('');

  // Filtered transactions
  var filtered=suphFilter==='all'?txns:txns.filter(function(t){return t.mod===suphFilter;});
  if(!filtered.length){
    $('suph-txn-list').innerHTML='<div class="empty" style="padding:30px"><div class="ei">📭</div><div class="et">No '+( suphFilter==='all'?'transactions':''+suphFilter+' records')+' for this supplier.</div></div>';
    return;
  }
  // Group by month
  var byMonth={};var monthOrder=[];
  filtered.forEach(function(t){
    var mk=t.date?t.date.substring(0,7):'unknown';
    if(!byMonth[mk]){byMonth[mk]=[];monthOrder.push(mk);}
    byMonth[mk].push(t);
  });
  var html='';
  monthOrder.forEach(function(mk){
    var label=mk==='unknown'?'Unknown Date':new Date(mk+'-01').toLocaleDateString('en-GB',{month:'long',year:'numeric'});
    var monthTotal=byMonth[mk].reduce(function(s,t){return s+t.cost;},0);
    html+='<div style="padding:7px 16px 4px;background:var(--cream);font-size:0.65rem;text-transform:uppercase;letter-spacing:.08em;color:var(--muted);display:flex;justify-content:space-between;border-bottom:1px solid var(--sand)">'
      +'<span>'+label+'</span>'+(monthTotal?'<span style="font-family:\'DM Mono\',monospace;color:var(--red)">'+rupees(monthTotal)+'</span>':'')+'</div>';
    byMonth[mk].forEach(function(t){
      var modColor={Feed:'var(--green)',Medicine:'var(--blue)',Brada:'var(--amber)',Flock:'var(--accent)'}[t.mod]||'var(--muted)';
      html+='<div style="display:flex;align-items:center;gap:12px;padding:10px 16px;border-bottom:1px solid var(--sand);transition:.1s" onmouseenter="this.style.background=\'var(--cream)\'" onmouseleave="this.style.background=\'\'">'
        +'<div style="width:30px;height:30px;border-radius:8px;background:var(--sand);display:flex;align-items:center;justify-content:center;font-size:0.85rem;flex-shrink:0">'+t.icon+'</div>'
        +'<div style="flex:1;min-width:0">'
          +'<div style="font-size:0.82rem;font-weight:500">'+esc(t.detail)+'</div>'
          +'<div style="font-size:0.7rem;color:var(--muted);margin-top:1px"><span style="color:'+modColor+';font-weight:600">'+esc(t.mod)+'</span> · '+fmt(t.date)+'</div>'
        +'</div>'
        +(t.cost?'<div style="font-family:\'DM Mono\',monospace;font-size:0.88rem;font-weight:600;color:var(--red);flex-shrink:0">'+rupees(t.cost)+'</div>':'')
      +'</div>';
    });
  });
  $('suph-txn-list').innerHTML=html;
}

function setSupFilter(sid,mod){suphFilter=mod;renderSupHistoryPanel(sid);}

$('suph-close-btn').addEventListener('click',function(){
  activeSupId=null;$('sup-history-panel').style.display='none';renderSuppliers();
});

// ════════════════════════════════════════════════════
//  US-012/013/014/015/016: FEED
// ════════════════════════════════════════════════════
async function adjustFeedStockInBackend(name, delta, minThreshold){
  var res = await api('/feed-types/stock',{
    method:'PATCH',
    headers:{'Content-Type':'application/json'},
    body:JSON.stringify({
      name:name,
      delta:delta,
      minThreshold:minThreshold
    })
  });
  if(!res.ok){
    var txt = await res.text().catch(function(){ return ''; });
    throw new Error(txt || 'Failed to update feed stock in backend.');
  }
}

async function applyRemainingFeedStockDelta(typeName, intendedDelta, minThreshold, beforeStock){
  await loadFeedTypesFromApi();
  var afterStock=(feedStock[typeName] && typeof feedStock[typeName].qty==='number')
    ? Number(feedStock[typeName].qty)
    : null;
  var backendApplied=(afterStock!==null)?(afterStock-beforeStock):0;
  var remainingDelta=intendedDelta-backendApplied;
  if((intendedDelta>=0 && remainingDelta>0) || (intendedDelta<0 && remainingDelta<0)){
    await adjustFeedStockInBackend(typeName, remainingDelta, minThreshold);
    await loadFeedTypesFromApi();
  }
}

async function updateFeedThresholdInBackend(feedTypeId, threshold){
  var res = await api('/feed-types/'+encodeURIComponent(feedTypeId)+'/threshold',{
    method:'PATCH',
    headers:{'Content-Type':'application/json'},
    body:JSON.stringify({minThreshold:threshold})
  });
  if(!res.ok){
    throw new Error('Failed to update feed threshold in backend.');
  }
}

function refreshFeedTypeSelects(){
  var types=Object.keys(feedStock);
  var lists=[{id:'fu-type',placeholder:'— Select —'},{id:'df-type',placeholder:'— Select —'}];
  lists.forEach(function(l){
    var sel=$(l.id);
    if(!sel)return;
    sel.innerHTML='<option value="">'+l.placeholder+'</option>';
    types.forEach(function(t){var o=document.createElement('option');o.value=t;o.textContent=t;sel.appendChild(o);});
  });
  var dl=$('feed-types-list');dl.innerHTML='';
  types.forEach(function(t){var o=document.createElement('option');o.value=t;dl.appendChild(o);});
}
async function ensureFeedTypeForPurchase(typeName, threshold){
  var normalized=(typeName||'').trim();
  if(!normalized) throw new Error('Feed type is required.');
  var existing=feedStock[normalized];
  if(existing && existing.id){
    return { id:String(existing.id), name:normalized };
  }
  var up=await api('/feed-types/upsert',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({
    name:normalized,
    minThreshold:threshold
  })});
  if(!up.ok){
    var txt=await up.text().catch(function(){return '';});
    throw new Error(txt || 'Could not create feed type in inventory.');
  }
  var body=await up.json().catch(function(){return null;});
  var fid=body && (body.id!=null || body.feedTypeId!=null) ? String(body.id!=null ? body.id : body.feedTypeId) : '';
  if(!fid) throw new Error('Feed type created but id missing in response.');
  return { id:fid, name:String(body.name||normalized) };
}
$('btn-feed-buy').addEventListener('click',function(){
  fillSupSelect('fb-sup',['Feed Supplier']);refreshFeedTypeSelects();$('fb-type').value='';$('fb-qty').value='';$('fb-cost').value='';$('fb-date').value=today();$('fb-total').textContent='₨ —';
  ['fg-fb-s','fg-fb-ft','fg-fb-q','fg-fb-c','fg-fb-d'].forEach(function(id){inv(id,false);});openM('m-feed-buy');
});
['fb-qty','fb-cost'].forEach(function(id){$(id).addEventListener('input',function(){var q=parseInt($('fb-qty').value)||0,c=parseInt($('fb-cost').value)||0;$('fb-total').textContent=q&&c?rupees(q*c):'₨ —';});});
$('do-feed-buy').addEventListener('click',async function(){
  var supId=$('fb-sup').value,type=$('fb-type').value.trim(),qty=parseInt($('fb-qty').value),cost=parseInt($('fb-cost').value),date=$('fb-date').value,ok=true;
  inv('fg-fb-s',!supId);if(!supId)ok=false;inv('fg-fb-ft',!type);if(!type)ok=false;
  inv('fg-fb-q',!qty||qty<1);if(!qty||qty<1)ok=false;inv('fg-fb-c',!cost||cost<1);if(!cost||cost<1)ok=false;
  inv('fg-fb-d',!date);if(!date)ok=false;if(!ok)return;
  var threshold=(feedStock[type] && feedStock[type].threshold!=null) ? feedStock[type].threshold : 5;
  try{
    var ft=await ensureFeedTypeForPurchase(type, threshold);
    var purchaseTypeName=ft.name || type;
    var beforeStock=(feedStock[purchaseTypeName] && typeof feedStock[purchaseTypeName].qty==='number')
      ? Number(feedStock[purchaseTypeName].qty)
      : 0;
    var pr=await api('/feed/purchases',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({
      feedTypeId:ft.id,
      supplierId:supId,
      purchaseDate:date,
      sacksQty:qty,
      costPerSack:cost
    })});
    if(!pr.ok){var pt=await pr.text().catch(function(){return '';});toast('Feed purchase failed: '+(pt||pr.status),'t-bad');return;}
    await loadFeedTypesFromApi();
    var afterStock=(feedStock[purchaseTypeName] && typeof feedStock[purchaseTypeName].qty==='number')
      ? Number(feedStock[purchaseTypeName].qty)
      : null;
    var backendApplied=(afterStock!==null)?Math.max(0, afterStock-beforeStock):0;
    var remainingDelta=Math.max(0, qty-backendApplied);
    if(remainingDelta>0){
      await adjustFeedStockInBackend(purchaseTypeName, remainingDelta, threshold);
      await loadFeedTypesFromApi();
    }
    await loadFeedTxnsFromApi();
    if(feedStock[purchaseTypeName]) feedStock[purchaseTypeName].lastPurchase = date;
  }catch(err){
    toast(err.message || 'Backend feed purchase failed.','t-bad');
    return;
  }
  var sup=suppliers.find(function(s){return s.id===supId;});
  addLog('Feed','Purchase',type+': '+qty+' sacks from '+(sup?sup.name:''));
  closeM('m-feed-buy');toast('Feed stock updated.','t-ok');refreshFeedTypeSelects();renderFeed();renderDash();
});
$('btn-feed-usage').addEventListener('click',function(){
  // Moved to Daily Records
  document.querySelectorAll('.nav-item').forEach(function(b){b.classList.remove('on');});
  document.querySelectorAll('.view').forEach(function(v){v.classList.remove('on');});
  var dailyBtn=document.querySelector('[data-view="daily"]');if(dailyBtn)dailyBtn.classList.add('on');
  var dailyView=$('view-daily');if(dailyView)dailyView.classList.add('on');
  renderDaily();
  $('btn-dfeed').click();
});
$('fu-type').addEventListener('change',function(){var s=feedStock[this.value];$('fu-hint').textContent=s?'Available: '+s.qty+' sacks':'';});
$('do-feed-use').addEventListener('click',async function(){
  var flockId=$('fu-flock').value,type=$('fu-type').value,qty=parseInt($('fu-qty').value),date=$('fu-date').value,ok=true;
  inv('fg-fu-f',!flockId);if(!flockId)ok=false;inv('fg-fu-ft',!type);if(!type)ok=false;
  inv('fg-fu-q',!qty||qty<1);if(!qty||qty<1)ok=false;inv('fg-fu-d',!date);if(!date)ok=false;
  if(!ok)return;
  var s=feedStock[type];
  var fidDb=flockDbUuid(flockId);
  var ft=feedStock[type];
  if(!fidDb||!ft||!ft.id){toast('Select a valid flock and feed type.','t-bad');return;}
  if(s&&qty>s.qty){if(!confirm('Using '+qty+' sacks but only '+s.qty+' available. Continue?'))return;}
  try{
    var threshold=(s && s.threshold!=null) ? s.threshold : 5;
    var beforeStock=(s && typeof s.qty==='number') ? Number(s.qty) : 0;
    var ur=await api('/feed/usage',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({
      flockId:fidDb,
      feedTypeId:ft.id,
      recordDate:date,
      sacksUsed:qty
    })});
    if(!ur.ok){var ut=await ur.text().catch(function(){return '';});toast('Feed usage failed: '+(ut||ur.status),'t-bad');return;}
    await applyRemainingFeedStockDelta(type, -qty, threshold, beforeStock);
    await loadFeedTxnsFromApi();
    if(feedStock[type]) feedStock[type].lastUsage = date;
  }catch(err){
    toast(err.message || 'Backend feed usage failed.','t-bad');
    return;
  }
  addLog(flockId,'Feed Usage',type+': '+qty+' sacks used');
  closeM('m-feed-use');toast('Feed usage saved.','t-ok');renderFeed();renderDash();
});
$('btn-feed-sale').addEventListener('click',function(){
  // Sales are managed under Flock Sales
  document.querySelectorAll('.nav-item').forEach(function(b){b.classList.remove('on');});
  document.querySelectorAll('.view').forEach(function(v){v.classList.remove('on');});
  var salesBtn=document.querySelector('[data-view="sales"]');if(salesBtn)salesBtn.classList.add('on');
  var salesView=$('view-sales');if(salesView)salesView.classList.add('on');
  renderSalesView();
  $('fsl-buyer').value='';$('fsl-qty').value='';$('fsl-price').value='';$('fsl-date').value=today();$('fsl-total').textContent='₨ —';
  ['fg-fsl-b','fg-fsl-q','fg-fsl-c','fg-fsl-d'].forEach(function(id){inv(id,false);});openM('m-feed-sale');
});
['fsl-qty','fsl-price'].forEach(function(id){$(id).addEventListener('input',function(){var q=parseInt($('fsl-qty').value)||0,p=parseInt($('fsl-price').value)||0;$('fsl-total').textContent=q&&p?rupees(q*p):'₨ —';});});
$('do-feed-sale').addEventListener('click',async function(){
  var buyer=$('fsl-buyer').value.trim(),qty=parseInt($('fsl-qty').value),price=parseInt($('fsl-price').value),date=$('fsl-date').value,ok=true;
  inv('fg-fsl-b',!buyer);if(!buyer)ok=false;
  inv('fg-fsl-q',!qty||qty<1);if(!qty||qty<1)ok=false;inv('fg-fsl-c',!price||price<1);if(!price||price<1)ok=false;
  inv('fg-fsl-d',!date);if(!date)ok=false;if(!ok)return;
  try{
    var sr=await api('/feed/sales',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({
      saleDate:date,
      sacksQty:qty,
      pricePerSack:price,
      buyerName:buyer
    })});
    if(!sr.ok){var st=await sr.text().catch(function(){return '';});toast('Sacks sale failed: '+(st||sr.status),'t-bad');return;}
    await loadFeedTxnsFromApi();
  }catch(err){
    toast(err.message || 'Backend sacks sale failed.','t-bad');
    return;
  }
  var rev=qty*price;
  addLog('Sales','Sacks Sale',qty+' sacks sold to '+buyer+' for '+rupees(rev));
  closeM('m-feed-sale');toast('Sacks sale recorded successfully.','t-ok');renderFeed();renderSalesView();renderDash();
});
var activeFeedTab='all';
document.querySelectorAll('[data-ftab]').forEach(function(btn){
  btn.addEventListener('click',function(){
    document.querySelectorAll('[data-ftab]').forEach(function(b){b.classList.remove('on');});
    btn.classList.add('on');activeFeedTab=btn.getAttribute('data-ftab');renderFeedTxns();
  });
});
function renderFeedTxns(){
  var rows=feedTxns.filter(function(t){return activeFeedTab==='all'||t.type.toLowerCase()===activeFeedTab;});
  rows.sort(function(a,b){return b.date.localeCompare(a.date);});
  $('feed-txn-tbody').innerHTML=rows.length?rows.map(function(t){
    var typeColor=t.type==='Purchase'?'var(--green)':t.type==='Usage'?'var(--accent)':'var(--blue)';
    var who=t.type==='Usage'&&t.shift?((t.flockId||'—')+' · '+t.shift):(t.flockId||t.supName||t.buyer||'—');
    return '<tr><td>'+fmt(t.date)+'</td><td><strong style="color:'+typeColor+'">'+esc(t.type)+'</strong></td><td>'+esc(t.feedType)+'</td>'
      +'<td style="color:var(--muted)">'+esc(who)+'</td><td>'+t.qty+'</td><td>'+(t.cost?rupees(t.cost):'—')+'</td></tr>';
  }).join(''):'<tr><td colspan="6"><div class="empty" style="padding:20px"><div class="et">No transactions.</div></div></td></tr>';
}
function renderFeed(){
  // Stock table
  var types=Object.keys(feedStock);
  $('feed-stock-tbody').innerHTML=types.length?types.map(function(t){
    var s=feedStock[t];var isLow=s.qty<=s.threshold;
    return '<tr><td><strong>'+esc(t)+'</strong></td><td style="font-family:\'DM Mono\',monospace;font-size:1rem">'+s.qty+'</td>'
      +'<td><input class="fi feed-threshold" data-feed-type="'+esc(t)+'" type="number" min="0" value="'+(s.threshold||0)+'" style="width:90px;height:34px;padding:6px 10px;font-size:0.82rem"></td>'
      +'<td><span class="'+(s.qty===0?'stock-zero':isLow?'stock-warn':'stock-ok')+'">'+(s.qty===0?'Out of Stock':isLow?'Low Stock':'OK')+'</span></td>'
      +'<td>'+fmt(s.lastPurchase)+'</td><td>'+fmt(s.lastUsage)+'</td></tr>';
  }).join(''):'<tr><td colspan="6"><div class="empty" style="padding:20px"><div class="et">No feed types yet. Purchase feed to add types.</div></div></td></tr>';
  renderFeedTxns();
}

document.addEventListener('change',async function(e){
  var el=e.target;
  if(!el||!el.classList||!el.classList.contains('feed-threshold'))return;
  var t=el.getAttribute('data-feed-type');
  if(!t||!feedStock[t])return;
  var v=parseInt(el.value,10);
  if(isNaN(v)||v<0)v=0;
  var id = feedStock[t].id;
  if(!id){
    toast('Cannot update threshold: feed type id missing.','t-bad');
    return;
  }
  try{
    await updateFeedThresholdInBackend(id, v);
    feedStock[t].threshold=v;
  }catch(_err){
    toast('Failed to update threshold in backend.','t-bad');
    return;
  }
  renderFeed();renderDash();
});

// ════════════════════════════════════════════════════
//  DAILY FEED + DAILY MEDICINE (Daily Records)
// ════════════════════════════════════════════════════
function calcDfTotal(){
  var d=parseInt($('df-day').value,10);if(isNaN(d)||d<0)d=0;
  var n=parseInt($('df-night').value,10);if(isNaN(n)||n<0)n=0;
  $('df-total').textContent=String(d+n);
}
function updateDfAvail(){
  var t=$('df-type').value;
  var s=t&&feedStock[t];
  $('df-av').textContent=s?String(s.qty)+' sacks':'—';
}
$('btn-dfeed').addEventListener('click',function(){
  fillFlockSelect('df-flock');
  refreshFeedTypeSelects();
  $('df-date').value=today();
  $('df-type').value='';
  $('df-day').value='';$('df-night').value='';
  $('df-total').textContent='—';$('df-av').textContent='—';
  ['fg-df-f','fg-df-d','fg-df-t','fg-df-day','fg-df-night'].forEach(function(id){inv(id,false);});
  openM('m-dfeed');
});
['df-day','df-night'].forEach(function(id){$(id).addEventListener('input',calcDfTotal);});
$('df-type').addEventListener('change',updateDfAvail);
$('do-dfeed').addEventListener('click',async function(){
  var flockId=$('df-flock').value,date=$('df-date').value,type=$('df-type').value;
  var day=parseInt($('df-day').value,10),night=parseInt($('df-night').value,10);
  if(isNaN(day)||day<0)day=0;
  if(isNaN(night)||night<0)night=0;
  var total=day+night;
  var ok=true;
  inv('fg-df-f',!flockId);if(!flockId)ok=false;
  inv('fg-df-d',!date);if(!date)ok=false;
  inv('fg-df-t',!type);if(!type)ok=false;
  inv('fg-df-day',day<0);inv('fg-df-night',night<0);
  if(!ok)return;
  if(total<1){toast('Enter day and/or night sacks.','t-bad');return;}
  var s=feedStock[type];
  var fidDb=flockDbUuid(flockId);
  var ft=feedStock[type];
  if(!fidDb||!ft||!ft.id){toast('Select a valid flock and feed type.','t-bad');return;}
  if(s&&total>s.qty){if(!confirm('Using '+total+' sacks but only '+s.qty+' available. Continue?'))return;}
  try{
    var threshold=(s && s.threshold!=null) ? s.threshold : 5;
    var beforeStock=(s && typeof s.qty==='number') ? Number(s.qty) : 0;
    if(day>0){
      var drDay=await api('/feed/usage',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({
        flockId:fidDb,
        feedTypeId:ft.id,
        recordDate:date,
        sacksUsed:day,
        shift:'DAY'
      })});
      if(!drDay.ok){var dtDay=await drDay.text().catch(function(){return '';});toast('Daily feed usage (day) failed: '+(dtDay||drDay.status),'t-bad');return;}
    }
    if(night>0){
      var drNight=await api('/feed/usage',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({
        flockId:fidDb,
        feedTypeId:ft.id,
        recordDate:date,
        sacksUsed:night,
        shift:'NIGHT'
      })});
      if(!drNight.ok){var dtNight=await drNight.text().catch(function(){return '';});toast('Daily feed usage (night) failed: '+(dtNight||drNight.status),'t-bad');return;}
    }
    await applyRemainingFeedStockDelta(type, -total, threshold, beforeStock);
    await loadFeedTxnsFromApi();
    if(feedStock[type]) feedStock[type].lastUsage = date;
  }catch(err){
    toast(err.message || 'Backend daily feed failed.','t-bad');
    return;
  }
  dailyFeedRecords.push({id:'DF-'+String(ftSeq++).padStart(4,'0'),flockId:flockId,date:date,feedType:type,daySacks:day,nightSacks:night,totalSacks:total,time:now(),editor:EDITOR});
  addLog(flockId,'Daily Feed',type+': Day '+day+', Night '+night+' sacks (Total '+total+')');
  closeM('m-dfeed');toast('Daily feed saved.','t-ok');renderFeed();renderDaily();renderDash();
});

function dmUnitOptions(){
  return ['bottle','ml','kg','g','sachet','tablet','pack','other'];
}
function dmMedicineOptionsHtml(){
  var names=Object.keys(medStock).sort(function(a,b){ return a.localeCompare(b); });
  var opts='<option value="">— Select medicine —</option>';
  names.forEach(function(n){
    opts += '<option value="'+esc(n)+'">'+esc(n)+'</option>';
  });
  return opts;
}
function dmRowHtml(idx){
  var opts=dmUnitOptions().map(function(u){return '<option value="'+u+'">'+u+'</option>';}).join('');
  return '<div class="fr" data-idx="'+idx+'" style="margin-bottom:8px">'
    +'<div class="fg" style="flex:1"><label class="fl">Medicine Name *</label><select class="fi dm-name">'+dmMedicineOptionsHtml()+'</select><div class="fhint dm-stock-hint">Available: —</div></div>'
    +'<div class="fg" style="width:110px"><label class="fl">Qty *</label><input class="fi dm-qty" type="number" min="0" step="0.01"></div>'
    +'<div class="fg" style="width:130px"><label class="fl">Unit *</label><select class="fi dm-unit">'+opts+'</select></div>'
    +'</div>'
    +'<div class="fr" data-idx="'+idx+'" style="margin-top:-6px;margin-bottom:12px">'
    +'<div class="fg" style="flex:1"><label class="fl">Usage Time *</label><input class="fi dm-time" placeholder="e.g. 08:30 AM / Morning"></div>'
    +'<div class="fg dm-other-wrap" style="display:none;flex:1"><label class="fl">Other Unit</label><input class="fi dm-other" placeholder="Type unit"></div>'
    +'</div>';
}
function dmReindex(){
  // no-op for now (keeps layout consistent)
}
function knownMedicineUnits(){
  return ['bottle','ml','kg','g','sachet','tablet','pack','other'];
}
function splitMedicineUnitValue(value){
  var v=(value||'').trim();
  if(!v)return {unit:'',unitOther:''};
  return knownMedicineUnits().indexOf(v)>=0 && v!=='other'
    ? {unit:v,unitOther:''}
    : {unit:'other',unitOther:v};
}
function composeMedicineUnitValue(unit, unitOther){
  if(unit==='other'){
    return (unitOther||'').trim();
  }
  return (unit||'').trim();
}
function dmApplyUnitForRow(nameInput){
  if(!nameInput)return;
  var row=nameInput.closest('.fr');
  if(!row)return;
  var unitSel=row.querySelector('.dm-unit');
  var hint=row.querySelector('.dm-stock-hint');
  var wrap=row.nextElementSibling;
  if(!unitSel||!wrap)return;
  var otherWrap=wrap.querySelector('.dm-other-wrap');
  var otherInput=wrap.querySelector('.dm-other');
  var medName=(nameInput.value||'').trim();
  var s=medStock[medName];
  if(hint){
    hint.textContent=(s&&typeof s.qty==='number')
      ? ('Available: '+s.qty+' units')
      : 'Available: —';
  }
  if(!s)return;
  var parsed=splitMedicineUnitValue(s.unit);
  unitSel.value=parsed.unit||'other';
  if(otherInput)otherInput.value=parsed.unitOther||'';
  if(otherWrap)otherWrap.style.display=unitSel.value==='other'?'':'none';
}
function dmAttachRowEvents(root){
  root.querySelectorAll('.dm-unit').forEach(function(sel){
    if(sel.dataset.bound==='1')return;
    sel.dataset.bound='1';
    sel.addEventListener('change',function(){
      var wrap=sel.closest('.fr').nextElementSibling;
      if(!wrap)return;
      var otherWrap=wrap.querySelector('.dm-other-wrap');
      if(otherWrap)otherWrap.style.display=(sel.value==='other')?'':'none';
    });
  });
  root.querySelectorAll('.dm-name').forEach(function(input){
    if(input.dataset.bound==='1')return;
    input.dataset.bound='1';
    input.addEventListener('input',function(){dmApplyUnitForRow(input);});
    input.addEventListener('change',function(){dmApplyUnitForRow(input);});
    input.addEventListener('blur',function(){dmApplyUnitForRow(input);});
  });
}
$('btn-dmed').addEventListener('click',function(){
  fillFlockSelect('dm-flock');
  refreshMedSelects();
  $('dm-date').value=today();
  $('dm-notes').value='';
  $('dm-items').innerHTML='';
  $('dm-items').insertAdjacentHTML('beforeend',dmRowHtml(0));
  dmAttachRowEvents($('dm-items'));
  ['fg-dm-f','fg-dm-d'].forEach(function(id){inv(id,false);});
  openM('m-dmed');
});
$('dm-add').addEventListener('click',function(e){
  e.preventDefault();
  var idx=$('dm-items').querySelectorAll('[data-idx]').length;
  $('dm-items').insertAdjacentHTML('beforeend',dmRowHtml(idx));
  dmAttachRowEvents($('dm-items'));
  dmReindex();
});
$('do-dmed').addEventListener('click',async function(){
  var flockId=$('dm-flock').value,date=$('dm-date').value,notes=$('dm-notes').value.trim();
  var ok=true;
  inv('fg-dm-f',!flockId);if(!flockId)ok=false;
  inv('fg-dm-d',!date);if(!date)ok=false;
  if(!ok)return;
  var items=[];
  var blocks=$('dm-items').querySelectorAll('div.fr[data-idx]');
  for(var i=0;i<blocks.length;i+=2){
    var top=blocks[i],bottom=blocks[i+1];
    if(!top||!bottom)continue;
    var name=(top.querySelector('.dm-name')||{}).value||'';
    name=name.trim();
    var qty=parseFloat((top.querySelector('.dm-qty')||{}).value);
    var unit=(top.querySelector('.dm-unit')||{}).value||'';
    var usageTime=((bottom.querySelector('.dm-time')||{}).value||'').trim();
    var unitOther=((bottom.querySelector('.dm-other')||{}).value||'').trim();
    if(!name)continue;
    if(!qty||qty<=0){toast('Medicine qty must be > 0 for '+name,'t-bad');return;}
    if(!unit){toast('Select a unit for '+name,'t-bad');return;}
    if(!usageTime){toast('Select usage time for '+name,'t-bad');return;}
    if(unit==='other'&&!unitOther){toast('Provide other unit for '+name,'t-bad');return;}
    items.push({name:name,qty:qty,unit:composeMedicineUnitValue(unit,unitOther),usageTime:usageTime});
  }
  if(!items.length){toast('Add at least one medicine item.','t-bad');return;}
  for(var j=0;j<items.length;j++){
    var item = items[j];
    var existing = medStock[item.name];
    if(existing && item.qty > existing.qty){
      toast('Insufficient stock for '+item.name+' ('+existing.qty+' available)','t-bad');
      return;
    }
  }
  var fidDb=flockDbUuid(flockId);
  if(!fidDb){toast('Flock is not linked to backend.','t-bad');return;}
  try{
    for(var k=0;k<items.length;k++){
      var it=items[k];
      var stockItem = medStock[it.name];
      var mid=stockItem&&stockItem.id;
      if(!mid){toast('Medicine '+it.name+' has no backend id — refresh medicine list.','t-bad');return;}
      var ur=await api('/medicine/usage',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({
        flockId:fidDb,
        medicineId:mid,
        recordDate:date,
        dosage:it.qty,
        unit:it.unit,
        usageTime:it.usageTime||null,
        usage_time:it.usageTime||null,
        notes:notes||null
      })});
      if(!ur.ok){var ut=await ur.text().catch(function(){return '';});toast('Medicine usage failed for '+it.name+': '+(ut||ur.status),'t-bad');return;}
      await adjustMedicineStockInBackend(
        it.name,
        -it.qty,
        stockItem ? stockItem.threshold : 5,
        it.unit || (stockItem ? stockItem.unit : null)
      );
    }
    await loadMedicinesFromApi();
    await loadMedTxnsFromApi();
  }catch(err){
    toast(err.message || 'Backend daily medicine failed.','t-bad');
    return;
  }
  dailyMedRecords.push({id:'DM-'+String(mdSeq++).padStart(4,'0'),flockId:flockId,date:date,items:items,notes:notes,time:now(),editor:EDITOR});
  items.forEach(function(it){
    var key=it.name;
    var s=medStock[key];
    if(s&&typeof s.qty==='number') s.lastUpdated=date;
  });
  addLog(flockId,'Daily Medicine',items.length+' item(s) on '+fmt(date));
  closeM('m-dmed');toast('Daily medicine saved.','t-ok');refreshMedSelects();renderMedicine();renderDaily();renderDash();
});

// ════════════════════════════════════════════════════
//  US-017/018/019: MEDICINE
// ════════════════════════════════════════════════════
async function adjustMedicineStockInBackend(name, delta, minThreshold, unit){
  var res = await api('/medicines/stock',{
    method:'PATCH',
    headers:{'Content-Type':'application/json'},
    body:JSON.stringify({
      name:name,
      delta:delta,
      minThreshold:minThreshold,
      unit:unit||null
    })
  });
  if(!res.ok){
    var txt = await res.text().catch(function(){ return ''; });
    throw new Error(txt || 'Failed to update medicine stock in backend.');
  }
  var raw = await res.text().catch(function(){ return ''; });
  if(!raw) return null;
  try{
    return JSON.parse(raw);
  }catch(_e){
    return null;
  }
}

function refreshMedSelects(){
  var meds=Object.keys(medStock);
  var dl=$('med-names-list');dl.innerHTML='';
  meds.forEach(function(m){var o=document.createElement('option');o.value=m;dl.appendChild(o);});
}
$('btn-med-buy').addEventListener('click',function(){
  fillSupSelect('mb-sup',['Medicine Supplier']);refreshMedSelects();
  $('mb-name').value='';$('mb-unit').value='';$('mb-unit-other').value='';$('fg-mb-ou').style.display='none';
  $('mb-qty').value='';$('mb-cost').value='';$('mb-date').value=today();$('mb-thresh').value=5;$('mb-total').textContent='₨ —';
  ['fg-mb-m','fg-mb-s','fg-mb-q','fg-mb-c','fg-mb-d'].forEach(function(id){inv(id,false);});openM('m-med-buy');
});
$('mb-unit').addEventListener('change',function(){
  $('fg-mb-ou').style.display=this.value==='other'?'':'none';
});
['mb-qty','mb-cost'].forEach(function(id){$(id).addEventListener('input',function(){var q=parseInt($('mb-qty').value)||0,c=parseInt($('mb-cost').value)||0;$('mb-total').textContent=q&&c?rupees(q*c):'₨ —';});});
$('do-med-buy').addEventListener('click',async function(){
  var name=$('mb-name').value.trim(),supId=$('mb-sup').value,unit=$('mb-unit').value,unitOther=$('mb-unit-other').value.trim();
  var qty=parseInt($('mb-qty').value),cost=parseInt($('mb-cost').value),date=$('mb-date').value,thresh=parseInt($('mb-thresh').value)||5,ok=true;
  inv('fg-mb-m',!name);if(!name)ok=false;inv('fg-mb-s',!supId);if(!supId)ok=false;
  inv('fg-mb-u',!unit);if(!unit)ok=false;
  inv('fg-mb-ou',unit==='other'&&!unitOther);if(unit==='other'&&!unitOther)ok=false;
  inv('fg-mb-q',!qty||qty<1);if(!qty||qty<1)ok=false;inv('fg-mb-c',!cost||cost<1);if(!cost||cost<1)ok=false;
  inv('fg-mb-d',!date);if(!date)ok=false;if(!ok)return;
  var resolvedUnit=composeMedicineUnitValue(unit,unitOther);
  try{
    await adjustMedicineStockInBackend(name, qty, thresh, resolvedUnit);
    await loadMedicinesFromApi();
    var pr=await api('/medicine/purchases',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({
      medicineName:name,
      supplierId:supId,
      purchaseDate:date,
      quantity:qty,
      unitCost:cost,
      unit:resolvedUnit
    })});
    if(!pr.ok){
      var pt=await pr.text().catch(function(){return '';});
      var msg=pt||String(pr.status);
      try{var ej=JSON.parse(pt);if(ej&&ej.detail)msg=ej.detail;if(ej&&ej.error&&!ej.detail)msg=ej.error;}catch(_e){}
      toast('Medicine purchase ledger failed: '+msg,'t-bad');return;
    }
    if(!medStock[name]) medStock[name]={qty:0,threshold:thresh,supplier:'',supId:supId,lastUpdated:null,unit:resolvedUnit};
    medStock[name].supId=supId;
    medStock[name].lastUpdated=date;
    medStock[name].unit=resolvedUnit;
    await loadMedTxnsFromApi();
  }catch(err){
    toast(err.message || 'Backend medicine purchase failed.','t-bad');
    return;
  }
  var sup=suppliers.find(function(s){return s.id===supId;});
  if(sup)medStock[name].supplier=sup.name;
  addLog('Medicine','Purchase',name+': '+qty+' '+resolvedUnit+' from '+(sup?sup.name:''));
  closeM('m-med-buy');toast('Medicine purchased successfully.','t-ok');refreshMedSelects();renderMedicine();renderDash();
});
function renderMedicine(){
  var meds=Object.keys(medStock);
  $('med-stock-tbody').innerHTML=meds.length?meds.map(function(m){
    var s=medStock[m];var isLow=s.qty<=s.threshold;
    return '<tr><td><strong>'+esc(m)+'</strong></td><td style="font-family:\'DM Mono\',monospace">'+s.qty+'</td><td>'+s.threshold+'</td>'
      +'<td><span class="'+(s.qty===0?'stock-zero':isLow?'stock-warn':'stock-ok')+'">'+(s.qty===0?'Out of Stock':isLow?'⚠️ Low':'✅ OK')+'</span></td>'
      +'<td>'+esc(s.supplier||'—')+'</td><td>'+fmt(s.lastUpdated)+'</td></tr>';
  }).join(''):'<tr><td colspan="6"><div class="empty" style="padding:20px"><div class="et">No medicines yet.</div></div></td></tr>';
  var rows=medTxns.slice().sort(function(a,b){return b.date.localeCompare(a.date);});
  $('med-txn-tbody').innerHTML=rows.length?rows.map(function(t){
    var typeColor=t.type==='Purchase'?'var(--green)':'var(--accent)';
    var who=t.type==='Usage' && t.usageTime
      ? (t.flockId||'—')+' @ '+t.usageTime
      : (t.flockId||t.supName||'—');
    return '<tr><td>'+fmt(t.date)+'</td><td><strong style="color:'+typeColor+'">'+esc(t.type)+'</strong></td><td>'+esc(t.medicine)+'</td>'
      +'<td style="color:var(--muted)">'+esc(who)+'</td><td>'+t.qty+' '+esc(t.unit||'units')+'</td><td>'+(t.cost?rupees(t.cost):'—')+'</td></tr>';
  }).join(''):'<tr><td colspan="6"><div class="empty" style="padding:20px"><div class="et">No records.</div></div></td></tr>';
}

// ════════════════════════════════════════════════════
//  US-009/010/011: BRADA
// ════════════════════════════════════════════════════
$('btn-brada-buy').addEventListener('click',function(){
  fillSupSelect('bb-sup',['Other']);fillFlockSelect('bb-flock');$('bb-qty').value='';$('bb-cost').value='';$('bb-date').value=today();$('bb-total').textContent='₨ —';
  ['fg-bb-f','fg-bb-s','fg-bb-q','fg-bb-c','fg-bb-d'].forEach(function(id){inv(id,false);});openM('m-brada-buy');
});
['bb-qty','bb-cost'].forEach(function(id){$(id).addEventListener('input',function(){var q=parseInt($('bb-qty').value)||0,c=parseInt($('bb-cost').value)||0;$('bb-total').textContent=q&&c?rupees(q*c):'₨ —';});});
$('do-brada-buy').addEventListener('click',async function(){
  var flockId=$('bb-flock').value,supId=$('bb-sup').value,qty=parseInt($('bb-qty').value),cost=parseInt($('bb-cost').value),date=$('bb-date').value,ok=true;
  inv('fg-bb-f',!flockId);if(!flockId)ok=false;
  inv('fg-bb-s',!supId);if(!supId)ok=false;inv('fg-bb-q',!qty||qty<1);if(!qty||qty<1)ok=false;
  inv('fg-bb-c',!cost||cost<1);if(!cost||cost<1)ok=false;inv('fg-bb-d',!date);if(!date)ok=false;if(!ok)return;
  var fidDb=flockDbUuid(flockId);
  if(!fidDb){toast('Flock is not linked to backend.','t-bad');return;}
  try{
    var br=await api('/brada/purchases',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({
      flockId:fidDb,
      supplierId:supId,
      purchaseDate:date,
      quantity:qty,
      unitCost:cost
    })});
    if(!br.ok){var bt=await br.text().catch(function(){return '';});toast('Brada purchase failed: '+(bt||br.status),'t-bad');return;}
    await loadBradaFromApi();
  }catch(_e){
    toast('Backend not reachable for brada.','t-bad');
    return;
  }
  var sup=suppliers.find(function(s){return s.id===supId;});
  addLog('Brada','Purchase',qty+' bags from '+(sup?sup.name:'')+' for '+rupees(qty*cost));
  closeM('m-brada-buy');toast('Brada purchase recorded.','t-ok');renderBrada();renderDash();
});
$('btn-brada-use').addEventListener('click',function(){
  toast('Brada usage tracking is disabled. Use Purchase Brada only.','t-info');
});
$('do-brada-use').addEventListener('click',function(){
  toast('Brada usage tracking is disabled.','t-info');
  closeM('m-brada-use');
});
/** Purchases add bags; Usage rows (if any) subtract. Current stock = total purchased − total used. */
function computeBradaInventory(){
  var totalIn=bradaTxns.filter(function(t){return t.type==='Purchase';}).reduce(function(s,t){return s+(t.qty||0);},0);
  var totalOut=bradaTxns.filter(function(t){return t.type==='Usage';}).reduce(function(s,t){return s+(t.qty||0);},0);
  var stock=Math.max(0,totalIn-totalOut);
  var totalCost=bradaTxns.filter(function(t){return t.type==='Purchase';}).reduce(function(s,t){return s+(t.cost||0);},0);
  var merged=bradaTxns.slice().sort(function(a,b){
    var c=(a.date||'').localeCompare(b.date||'');
    if(c!==0)return c;
    return String(a.id||'').localeCompare(String(b.id||''));
  });
  var bal=0;
  var rowsDesc=[];
  merged.forEach(function(t){
    if(t.type==='Purchase')bal+=t.qty||0;
    else if(t.type==='Usage')bal-=t.qty||0;
    rowsDesc.push({t:t, balAfter:bal});
  });
  rowsDesc.reverse();
  return {totalIn:totalIn,totalOut:totalOut,stock:stock,totalCost:totalCost,rows:rowsDesc};
}
function renderBrada(){
  var inv=computeBradaInventory();
  bradaStock=inv.stock;
  $('brada-stock-val').textContent=String(inv.stock);
  $('brada-total-in').textContent=String(inv.totalIn);
  $('brada-total-out').textContent=String(inv.totalOut);
  $('brada-total-cost').textContent=rupees(inv.totalCost);
  $('brada-tbody').innerHTML=inv.rows.length?inv.rows.map(function(row){
    var t=row.t;
    var typeColor=t.type==='Purchase'?'var(--green)':'var(--accent)';
    var who=t.type==='Purchase'
      ? esc(t.flockId||'—')+' · '+esc(t.supName||'—')
      : esc(t.flockId||'—');
    return '<tr><td>'+fmt(t.date)+'</td><td><strong style="color:'+typeColor+'">'+esc(t.type)+'</strong></td>'
      +'<td style="color:var(--muted)">'+who+'</td>'
      +'<td>'+t.qty+'</td><td>'+(t.cost?rupees(t.cost):'—')+'</td><td style="font-weight:600;font-family:\'DM Mono\',monospace">'+row.balAfter+'</td></tr>';
  }).join(''):'<tr><td colspan="6"><div class="empty" style="padding:20px"><div class="et">No brada transactions yet.</div></div></td></tr>';
}

// ════════════════════════════════════════════════════
//  US-026: EXPENSES
// ════════════════════════════════════════════════════
$('btn-add-exp').addEventListener('click',function(){
  fillFlockSelect('ex-flock');$('ex-date').value=today();$('ex-cat').value='';
  $('ex-units').value='';$('ex-rate').value='';$('ex-amount').textContent='₨ —';$('ex-desc').value='';
  ['fg-ex-cat','fg-ex-d','fg-ex-u','fg-ex-r','fg-ex-desc'].forEach(function(id){inv(id,false);});
  // Fill flock selector with all flocks
  var sel=$('ex-flock');sel.innerHTML='<option value="">— Not linked —</option>';
  flocks.forEach(function(f){var o=document.createElement('option');o.value=f.id;o.textContent=f.id+' — '+f.breed;sel.appendChild(o);});
  openM('m-exp');
});
function calcExpAmount(){
  var u=parseFloat($('ex-units').value)||0;
  var r=parseFloat($('ex-rate').value)||0;
  var a=u*r;
  $('ex-amount').textContent=a>0?rupees(a):'₨ —';
}
['ex-units','ex-rate'].forEach(function(id){$(id).addEventListener('input',calcExpAmount);});
$('do-exp').addEventListener('click',async function(){
  var cat=$('ex-cat').value.trim(),date=$('ex-date').value;
  var units=parseFloat($('ex-units').value),rate=parseFloat($('ex-rate').value);
  var amount=(parseFloat(units)||0)*(parseFloat(rate)||0);
  var desc=$('ex-desc').value.trim(),ok=true;
  inv('fg-ex-cat',!cat);if(!cat)ok=false;inv('fg-ex-d',!date);if(!date)ok=false;
  inv('fg-ex-u',!units||units<=0);if(!units||units<=0)ok=false;
  inv('fg-ex-r',!rate||rate<=0);if(!rate||rate<=0)ok=false;
  inv('fg-ex-desc',!desc);if(!desc)ok=false;
  if(!ok)return;
  var flockUi=$('ex-flock').value;
  var fidDb=flockUi?flockDbUuid(flockUi):null;
  try{
    var body={
      category:uiExpenseToEnum(cat),
      amount:amount,
      description:desc,
      expenseDate:date
    };
    if(fidDb&&isLikelyUuid(fidDb))body.flockId=fidDb;
    var er=await api('/expenses',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
    if(!er.ok){var et=await er.text().catch(function(){return '';});toast('Expense failed: '+(et||er.status),'t-bad');return;}
    await loadExpensesFromApi();
  }catch(_e){
    toast('Backend not reachable for expense.','t-bad');
    return;
  }
  addLog('Expense','Added',cat+': '+rupees(amount)+' — '+desc);
  closeM('m-exp');toast('Expense saved.','t-ok');renderExpenses();renderDash();
});
function renderExpenses(){
  var filterCat=$('exp-filter-cat').value;
  var cats=[...new Set(expenses.map(function(e){return e.cat;}))];
  $('exp-filter-cat').innerHTML='<option value="">All Categories</option>';
  cats.forEach(function(c){var o=document.createElement('option');o.value=c;o.textContent=c;if(c===filterCat)o.selected=true;$('exp-filter-cat').appendChild(o);});
  var rows=expenses.filter(function(e){return !filterCat||e.cat===filterCat;});
  var total=expenses.reduce(function(s,e){return s+e.amount;},0);
  var now2=new Date();var ym=now2.getFullYear()+'-'+pad(now2.getMonth()+1);
  var monthTotal=expenses.filter(function(e){return e.date&&e.date.startsWith(ym);}).reduce(function(s,e){return s+e.amount;},0);
  $('exp-total').textContent=rupees(total);$('exp-month').textContent=rupees(monthTotal);$('exp-count').textContent=expenses.length;$('exp-cats').textContent=cats.length;
  rows.sort(function(a,b){return b.date.localeCompare(a.date);});
  $('exp-tbody').innerHTML=rows.length?rows.map(function(e){
    return '<tr><td>'+fmt(e.date)+'</td><td><span style="background:var(--al);color:var(--accent);padding:2px 8px;border-radius:10px;font-size:0.75rem">'+esc(e.cat)+'</span></td>'
      +'<td>'+esc(e.desc)+'</td><td style="color:var(--muted)">'+(e.flockId?'<span class="fc-id" style="font-size:0.75rem">'+esc(e.flockId)+'</span>':'—')+'</td>'
      +'<td style="color:var(--red);font-weight:600">'+rupees(e.amount)+'</td></tr>';
  }).join(''):'<tr><td colspan="5"><div class="empty" style="padding:20px"><div class="et">No expenses.</div></div></td></tr>';
}
$('exp-filter-cat').addEventListener('change',renderExpenses);

// ════════════════════════════════════════════════════
//  US-023/024/025: PAYROLL
// ════════════════════════════════════════════════════
$('btn-add-worker').addEventListener('click',function(){
  workerEditTarget=null;$('worker-modal-title').textContent='Add Worker';$('worker-edit-id').value='';
  $('wk-name').value='';$('wk-role').value='';$('wk-contact').value='';$('wk-join').value=today();$('wk-salary').value='';$('wk-status').value='Active';
  ['fg-wk-n','fg-wk-r','fg-wk-s'].forEach(function(id){inv(id,false);});openM('m-worker');
});
$('do-worker').addEventListener('click',async function(){
  var name=$('wk-name').value.trim(),role=$('wk-role').value.trim(),salary=parseFloat($('wk-salary').value),ok=true;
  inv('fg-wk-n',!name);if(!name)ok=false;inv('fg-wk-r',!role);if(!role)ok=false;
  inv('fg-wk-s',!salary||salary<=0);if(!salary||salary<=0)ok=false;if(!ok)return;
  var wid=$('worker-edit-id').value;
  var payload = {
    name:name,
    role:role,
    contact:$('wk-contact').value.trim(),
    joinDate:$('wk-join').value || today(),
    salaryRate:salary,
    isActive:$('wk-status').value === 'Active'
  };
  if(wid){
    try{
      var updateRes = await api('/workers/'+encodeURIComponent(wid),{
        method:'PUT',
        headers:{'Content-Type':'application/json'},
        body:JSON.stringify(payload)
      });
      if(!updateRes.ok){
        toast(updateRes.status === 400 ? 'Invalid data — check salary and join date.' : 'Failed to update worker in backend.','t-bad');
        return;
      }
      var updatedWorker = await updateRes.json();
      workers = workers.map(function(w0){return String(w0.id)===String(wid)?mapWorkerFromApi(updatedWorker):w0;});
    }catch(_err){
      toast('Backend not reachable. Could not update worker.','t-bad');
      return;
    }
    addLog('Payroll','Worker Edited',name);toast('Worker updated.','t-ok');
  } else {
    try{
      var createRes = await api('/workers',{
        method:'POST',
        headers:{'Content-Type':'application/json'},
        body:JSON.stringify(payload)
      });
      if(!createRes.ok){toast('Failed to add worker in backend.','t-bad');return;}
      var createdWorker = await createRes.json();
      workers.push(mapWorkerFromApi(createdWorker));
    }catch(_err){
      toast('Backend not reachable. Could not add worker.','t-bad');
      return;
    }
    addLog('Payroll','Worker Added',name+' — '+role+' @ '+rupees(salary)+'/mo');toast('Worker added.','t-ok');
  }
  closeM('m-worker');renderPayroll();
});
function editWorker(wid){
  var w=workers.find(function(x){return String(x.id)===String(wid);});if(!w)return;
  workerEditTarget=wid;$('worker-modal-title').textContent='Edit Worker';$('worker-edit-id').value=wid;
  $('wk-name').value=w.name;$('wk-role').value=w.role;$('wk-contact').value=w.contact||'';$('wk-join').value=w.join||'';$('wk-salary').value=w.salary;$('wk-status').value=w.status||'Active';
  ['fg-wk-n','fg-wk-r','fg-wk-s'].forEach(function(id){inv(id,false);});openM('m-worker');
}
$('btn-process-payroll').addEventListener('click',async function(){
  try{
    var res=await api('/workers');
    if(res.ok){
      var data=await res.json();
      workers=data.map(mapWorkerFromApi);
    }
  }catch(_err){
    // Keep existing in-memory workers if refresh fails.
  }
  $('pr-start').value='';$('pr-end').value=today();$('pr-days').value='';
  var firstActive=workers.find(function(w){return w.status==='Active';});
  payrollSelectedWorkerId=firstActive?String(firstActive.id):'';
  inv('fg-pr-workers',false);
  renderPayrollWorkerSelector();
  updatePayrollPreview();openM('m-payroll');
});
function renderPayrollWorkerSelector(){
  var active=workers.filter(function(w){return w.status==='Active';});
  var holder=$('pr-worker-list');
  if(!holder)return;
  if(!active.length){
    holder.innerHTML='<div class="empty" style="padding:14px"><div class="et">No active workers available.</div></div>';
    $('pr-worker-hint').textContent='No worker selected';
    return;
  }
  if(!payrollSelectedWorkerId){
    payrollSelectedWorkerId=String(active[0].id);
  }
  holder.innerHTML=active.map(function(w){
    var wid=String(w.id);
    var checked=payrollSelectedWorkerId===wid?' checked':'';
    return '<label style="display:flex;align-items:center;gap:8px;padding:6px 4px;cursor:pointer;border-bottom:1px solid var(--sand)">'
      +'<input type="radio" name="pr-worker-radio" class="pr-worker-check" data-wid="'+esc(wid)+'"'+checked+'>'
      +'<span style="font-weight:600">'+esc(w.name)+'</span>'
      +'<span style="font-size:0.75rem;color:var(--muted)">('+esc(w.role)+')</span>'
      +'<span style="margin-left:auto;font-family:\'DM Mono\',monospace;font-size:0.75rem">'+rupees(w.salary)+'</span>'
      +'</label>';
  }).join('');
  var selected=workers.find(function(w){return String(w.id)===String(payrollSelectedWorkerId);});
  $('pr-worker-hint').textContent=selected?('Selected: '+selected.name):'No worker selected';
}

function getSelectedPayrollWorker(){
  return workers.find(function(w){
    return w.status==='Active' && String(w.id)===String(payrollSelectedWorkerId);
  })||null;
}

function computeDaysWorked(start,end){
  if(!start||!end)return 0;
  var s=new Date(start+'T00:00:00');
  var e=new Date(end+'T00:00:00');
  var ms=e.getTime()-s.getTime();
  if(ms<0)return 0;
  return Math.floor(ms/86400000)+1;
}

function updatePayrollPreview(){
  var end=$('pr-end').value;
  var active=workers.filter(function(w){return w.status==='Active';});
  var selected=getSelectedPayrollWorker();
  var start=selected && selected.join ? selected.join : '';
  $('pr-start').value=start;
  var days=computeDaysWorked(start,end);
  $('pr-days').value=days?String(days):'';
  if(!active.length){$('payroll-preview').innerHTML='<div class="ab ab-warn"><span class="ab-ico">⚠️</span><div>No active workers.</div></div>';return;}
  if(!selected){$('payroll-preview').innerHTML='<div class="ab ab-warn"><span class="ab-ico">⚠️</span><div>Select one worker.</div></div>';return;}
  if(!start||!end||!days||days<=0){$('payroll-preview').innerHTML='<div class="ab ab-info"><span class="ab-ico">ℹ️</span><div>Select a worker, date range, and days worked to preview payroll.</div></div>';return;}
  if(end<start){$('payroll-preview').innerHTML='<div class="ab ab-warn"><span class="ab-ico">⚠️</span><div>End date must be after start date.</div></div>';return;}
  var total=(selected.salary/30)*days;
  $('payroll-preview').innerHTML='<div class="ab ab-info" style="margin-bottom:10px"><span class="ab-ico">📋</span><div>Processing payroll for <strong>'+fmt(start)+' → '+fmt(end)+'</strong> — Days: <strong>'+days+'</strong> — Worker: <strong>'+esc(selected.name)+'</strong> — Total: <strong>'+rupees(total)+'</strong></div></div>'
    +'<table><thead><tr><th>Worker</th><th>Role</th><th>Monthly</th><th>Per Day</th><th>Days</th><th>Total</th></tr></thead><tbody>'
    +function(){
      var perDay=selected.salary/30;
      var wTotal=perDay*days;
      return '<tr><td>'+esc(selected.name)+'</td><td style="color:var(--muted)">'+esc(selected.role)+'</td><td>'+rupees(selected.salary)+'</td><td>'+rupees(perDay.toFixed(2))+'</td><td>'+days+'</td><td style="font-weight:700">'+rupees(wTotal.toFixed(2))+'</td></tr>';
    }()
    +'</tbody></table>';
}
['pr-end'].forEach(function(id){$(id).addEventListener('change',updatePayrollPreview);$(id).addEventListener('input',updatePayrollPreview);});
$('pr-worker-list').addEventListener('change',function(e){
  if(!e.target.classList.contains('pr-worker-check'))return;
  var wid=String(e.target.getAttribute('data-wid'));
  payrollSelectedWorkerId=wid;
  renderPayrollWorkerSelector();
  updatePayrollPreview();
});
$('do-payroll').addEventListener('click',async function(){
  var selected=getSelectedPayrollWorker();
  var start=selected&&selected.join?selected.join:'';
  var end=$('pr-end').value,days=computeDaysWorked(start,end);
  inv('fg-pr-s',!start);if(!start)return;
  inv('fg-pr-e',!end);if(!end)return;
  inv('fg-pr-days',!days||days<=0);if(!days||days<=0)return;
  inv('fg-pr-workers',!selected);if(!selected)return;
  if(end<start){toast('End date must be after start date.','t-bad');return;}
  var payload={endDate:end,workerId:selected.id};
  var processed=null;
  try{
    var res=await api('/payroll/process',{
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body:JSON.stringify(payload)
    });
    if(!res.ok){
      var err='Failed to process payroll in backend.';
      try{
        var errText=await res.text();
        if(errText){
          try{
            var parsed=JSON.parse(errText);
            err=parsed.message||parsed.error||errText;
          }catch(_ignoreJson){
            err=errText;
          }
        }
      }catch(_ignore){}
      err='['+res.status+'] '+err;
      toast(err,'t-bad');
      return;
    }
    processed=await res.json();
    await loadPayrollRunsFromApi();
  }catch(_err){
    toast('Backend not reachable. Could not process payroll.','t-bad');
    return;
  }
  var total=Number(processed && processed.totalAmount ? processed.totalAmount : 0);
  // Add to expenses
  expenses.push({id:'EX-'+String(expSeq++).padStart(4,'0'),cat:'Labour',desc:'Payroll — '+fmt(start)+' → '+fmt(end)+' ('+days+' days)',units:days,rate:(total/Math.max(1,days)),amount:total,date:today(),flockId:'',time:now(),editor:EDITOR});
  addLog('Payroll','Processed',fmt(start)+' → '+fmt(end)+': '+selected.name+' total '+rupees(total));
  closeM('m-payroll');toast('Payroll processed: '+rupees(total),'t-ok');renderPayroll();renderExpenses();
});
function renderPayroll(){
  var active=workers.filter(function(w){return w.status==='Active';});
  var monthlytotal=active.reduce(function(s,w){return s+w.salary;},0);
  var allPaid=payrollRuns.reduce(function(s,r){return s+r.total;},0);
  $('pay-workers').textContent=active.length;$('pay-monthly').textContent=rupees(monthlytotal);$('pay-total').textContent=rupees(allPaid);$('pay-periods').textContent=payrollRuns.length;
  $('workers-tbody').innerHTML=workers.length?workers.map(function(w){
    var widAttr = esc(String(w.id));
    return '<tr><td><strong>'+esc(w.name)+'</strong></td><td style="color:var(--muted)">'+esc(w.role)+'</td><td style="color:var(--muted)">'+esc(w.contact||'—')+'</td>'
      +'<td style="font-family:\'DM Mono\',monospace">'+rupees(w.salary/30)+'</td><td style="font-family:\'DM Mono\',monospace">'+rupees(w.salary)+'</td>'
      +'<td><span class="badge '+(w.status==='Active'?'b-active':'b-closed')+'">'+esc(w.status)+'</span></td>'
      +'<td><div class="tbl-actions"><button type="button" class="btn btn-outline btn-sm worker-edit-btn" data-wid="'+widAttr+'">✏️ Edit</button></div></td></tr>';
  }).join(''):'<tr><td colspan="7"><div class="empty" style="padding:20px"><div class="et">No workers added yet.</div></div></td></tr>';
  $('payroll-tbody').innerHTML=payrollRuns.length?payrollRuns.slice().sort(function(a,b){return (b.end||'').localeCompare(a.end||'');}).map(function(r){
    var period=(r.start&&r.end)?(fmt(r.start)+' → '+fmt(r.end)+' ('+r.days+' days)'):(monthName(r.month)+' '+r.year);
    return '<tr><td><strong>'+period+'</strong></td><td>'+r.workers.length+'</td><td style="font-weight:600;color:var(--green)">'+rupees(r.total)+'</td><td><span class="badge b-processed">Processed</span></td><td style="color:var(--muted)">'+r.processedOn+'</td></tr>';
  }).join(''):'<tr><td colspan="5"><div class="empty" style="padding:20px"><div class="et">No payroll processed yet.</div></div></td></tr>';
}

// ════════════════════════════════════════════════════
//  REPORTS (US-028 to US-034 + US-020/021/025/027)
// ════════════════════════════════════════════════════
document.querySelectorAll('[data-rtab]').forEach(function(btn){
  btn.addEventListener('click',function(){
    document.querySelectorAll('[data-rtab]').forEach(function(b){b.classList.remove('on');});
    btn.classList.add('on');
    currentReportTab=btn.getAttribute('data-rtab');
    renderReport(currentReportTab);
  });
});
function fcrFeedPerLiveColor(fcr){
  if(fcr===null||fcr===undefined||!isFinite(fcr))return 'var(--muted)';
  if(fcr<=1.95)return 'var(--green)';
  if(fcr<=2.35)return 'var(--amber)';
  return 'var(--red)';
}
function renderReport(tab){
  currentReportTab=tab||currentReportTab||'mortality';
  var t=currentReportTab;
  var c=$('rpt-content');
  if(t==='mortality')c.innerHTML=buildMortalityReport();
  else if(t==='fcr')c.innerHTML=buildFCRReport();
  else if(t==='pnl')c.innerHTML=buildPnLReport();
  else if(t==='perf')c.innerHTML=buildPerfReport();
  else if(t==='sales-sum')c.innerHTML=buildSalesSummary();
  else if(t==='exp-sum')c.innerHTML=buildExpSummary();
  else if(t==='resource')c.innerHTML=buildResourceReport();
  else if(t==='med-supplier')c.innerHTML=buildMedSupplierReport();
  else if(t==='brada-rpt')c.innerHTML=buildBradaReport();
  else if(t==='feed-rpt')c.innerHTML=buildFeedReport();
}
function downloadReportPdf(){
  var tab=currentReportTab||'mortality';
  var names={mortality:'flockcontrol-mortality',fcr:'flockcontrol-fcr',pnl:'flockcontrol-pnl',perf:'flockcontrol-flock-performance','sales-sum':'flockcontrol-sales-summary','exp-sum':'flockcontrol-expense-summary',resource:'flockcontrol-resource','med-supplier':'flockcontrol-medicine-supplier','brada-rpt':'flockcontrol-brada','feed-rpt':'flockcontrol-feed'};
  var el=$('rpt-content');
  if(!el||!String(el.textContent||'').trim()){toast('Nothing to export for this report.','t-bad');return;}
  if(el.querySelector('.empty')&&!el.querySelector('.rpt-section')&&!el.querySelector('.kpi-grid')){toast('Nothing to export for this report.','t-bad');return;}
  if(!window.__html2pdf){toast('PDF library is still loading. Try again in a moment.','t-info');return;}
  var fn=(names[tab]||'flockcontrol-report')+'-'+new Date().toISOString().slice(0,10)+'.pdf';
  var opt={margin:[10,10,10,10],filename:fn,image:{type:'jpeg',quality:0.92},html2canvas:{scale:2,useCORS:true,logging:false},jsPDF:{unit:'mm',format:'a4',orientation:'portrait'},pagebreak:{mode:['avoid-all','css','legacy']}};
  window.__html2pdf().set(opt).from(el).save().catch(function(){toast('Could not generate PDF.','t-bad');});
}
document.addEventListener('click',function(e){
  if(e.target&&e.target.closest&&e.target.closest('#btn-rpt-pdf'))downloadReportPdf();
});
function buildMortalityReport(){
  if(!mortalities.length)return '<div class="empty" style="padding:40px"><div class="ei">📉</div><div class="et">No mortality records yet.</div></div>';
  var byFlock={};flocks.forEach(function(f){byFlock[f.id]={breed:f.breed,origQty:f.origQty,deaths:0,entries:[]};});
  mortalities.forEach(function(m){if(byFlock[m.flockId]){byFlock[m.flockId].deaths+=m.count;byFlock[m.flockId].entries.push(m);}});
  var html='<div class="rpt-section"><div class="rpt-header">Mortality Report — All Flocks</div>';
  html+='<table><thead><tr><th>Flock</th><th>Breed</th><th>Initial Qty</th><th>Total Deaths</th><th>Mortality Rate</th><th>Records</th></tr></thead><tbody>';
  Object.keys(byFlock).forEach(function(fid){
    var d=byFlock[fid];if(!d.entries.length)return;
    var rate=d.origQty?((d.deaths/d.origQty)*100).toFixed(2)+'%':'—';
    html+='<tr><td><span class="fc-id" style="font-size:0.75rem">'+esc(fid)+'</span></td><td>'+esc(d.breed)+'</td><td>'+d.origQty+'</td><td style="color:var(--red);font-weight:600">'+d.deaths+'</td><td>'+rate+'</td><td>'+d.entries.length+'</td></tr>';
  });
  html+='</tbody></table></div>';
  html+='<div class="rpt-section"><div class="rpt-header">Daily Mortality Entries</div><table><thead><tr><th>Date</th><th>Flock</th><th>Deaths</th><th>Cumulative</th><th>Notes</th></tr></thead><tbody>';
  mortalities.slice().sort(function(a,b){return b.date.localeCompare(a.date);}).forEach(function(m){
    html+='<tr><td>'+fmt(m.date)+'</td><td><span class="fc-id" style="font-size:0.75rem">'+esc(m.flockId)+'</span></td><td style="color:var(--red)">'+m.count+'</td><td>'+m.cumulative+'</td><td>'+esc(m.notes||'—')+'</td></tr>';
  });
  html+='</tbody></table></div>';return html;
}
function buildFCRReport(){
  var html='<div class="rpt-section"><div class="rpt-header">Feed Conversion Ratio (FCR) Report</div>';
  html+='<div class="ab ab-info"><span class="ab-ico">ℹ️</span><div>FCR matches <strong>weekly_summary.fcr_value</strong> in the database: <strong>feed (kg) ÷ live weight (kg)</strong>, with feed (kg) = cumulative feed sacks through the week end × 50. Lower values are generally better. Rows labeled <strong>Weekly summary</strong> use the stored snapshot; <strong>Estimated</strong> rows use the same formula from weight records and feed usage when no snapshot exists.</div></div>';
  var rows=[];
  if(weeklySummaries.length){
    weeklySummaries.slice().sort(function(a,b){
      var c=String(b.weekEndDate).localeCompare(String(a.weekEndDate));
      if(c!==0)return c;
      return (b.weekNumber||0)-(a.weekNumber||0);
    }).forEach(function(s){
      var f=flocks.find(function(x){return String(x.flockDbId)===String(s.flockDbId);});
      if(!f)return;
      var avgKg=s.avgWeightKg!=null?s.avgWeightKg:0;
      var feedKg=(s.cumulativeFeedSacks||0)*50;
      var liveKg=(s.remainingChicks||0)*avgKg;
      var fcr=s.fcrValue!=null&&!isNaN(s.fcrValue)?Number(s.fcrValue):fcrDbStyleFromMetrics(s.cumulativeFeedSacks,avgKg,s.remainingChicks);
      rows.push({src:'Weekly summary',flockId:f.id,breed:f.breed,weekEnd:s.weekEndDate,weekNum:s.weekNumber,remaining:s.remainingChicks,cumSacks:s.cumulativeFeedSacks,feedKg:feedKg,liveKg:liveKg,fcr:fcr});
    });
  }else if(weeklyRecords.length){
    weeklyRecords.slice().sort(function(a,b){return String(b.date).localeCompare(String(a.date));}).forEach(function(r){
      var f=flockByUiId(r.flockId);
      if(!f)return;
      var endD=r.date;
      var cum=cumulativeFeedSacksThrough(r.flockId,endD);
      var mort=totalMortalityForFlockThrough(r.flockId,endD);
      var rem=Math.max(0,(f.origQty||0)-mort);
      var avgKg=(r.avgWeightG||0)/1000;
      var feedKg=cum*50;
      var liveKg=rem*avgKg;
      var fcr=fcrDbStyleFromMetrics(cum,avgKg,rem);
      rows.push({src:'Estimated',flockId:r.flockId,breed:f.breed,weekEnd:endD,weekNum:weekNumberForFlockDate(r.flockId,endD),remaining:rem,cumSacks:cum,feedKg:feedKg,liveKg:liveKg,fcr:fcr});
    });
  }
  if(!rows.length){
    return html+'<div class="empty" style="padding:40px"><div class="ei">🔄</div><div class="et">No weekly summary snapshots yet. Add weekly summaries from Daily Records, or record weekly weights and feed usage to see estimated FCR.</div></div></div>';
  }
  html+='<table><thead><tr><th>Source</th><th>Flock</th><th>Breed</th><th>Week end</th><th>Wk#</th><th>Remaining</th><th>Cum. sacks</th><th>Feed (kg)</th><th>Live wt (kg)</th><th>FCR</th></tr></thead><tbody>';
  rows.forEach(function(row){
    var fcrColor=fcrFeedPerLiveColor(row.fcr);
    html+='<tr><td>'+esc(row.src)+'</td><td><span class="fc-id" style="font-size:0.75rem">'+esc(row.flockId)+'</span></td><td>'+esc(row.breed)+'</td><td>'+fmt(row.weekEnd)+'</td><td>'+(row.weekNum||'—')+'</td><td>'+(row.remaining||0)+'</td><td>'+(row.cumSacks||0)+'</td><td>'+(row.feedKg?row.feedKg.toFixed(1):'0')+'</td><td>'+(row.liveKg?row.liveKg.toFixed(1):'0')+'</td>'
      +'<td style="font-weight:700;color:'+fcrColor+'">'+(row.fcr!==null&&row.fcr!==undefined&&!isNaN(row.fcr)?Number(row.fcr).toFixed(3):'—')+'</td></tr>';
  });
  html+='</tbody></table></div>';return html;
}
function buildPnLReport(){
  var flockRev=flockSales.reduce(function(s,x){return s+x.total;},0);
  var otherRev=otherSales.reduce(function(s,x){return s+x.amount;},0);
  var sackRev=totalSackSaleRevenue();
  var totalRev=flockRev+otherRev+sackRev;
  var feedCost=feedTxns.filter(function(t){return t.type==='Purchase';}).reduce(function(s,t){return s+(t.cost||0);},0);
  var medCost=medTxns.filter(function(t){return t.type==='Purchase';}).reduce(function(s,t){return s+(t.cost||0);},0);
  var bradaCost=bradaTxns.filter(function(t){return t.type==='Purchase';}).reduce(function(s,t){return s+(t.cost||0);},0);
  var payrollCost=payrollRuns.reduce(function(s,r){return s+r.total;},0);
  var labourExp=labourExpenseForPnL();
  var otherExp=nonLabourExpenseForPnL();
  var totalCost=feedCost+medCost+bradaCost+payrollCost+otherExp+labourExp;
  var net=totalRev-totalCost;
  var html='<div class="kpi-grid">'
    +'<div class="kpi"><div class="kpi-lbl">Total Revenue</div><div class="kpi-val" style="color:var(--green)">'+rupees(totalRev)+'</div></div>'
    +'<div class="kpi"><div class="kpi-lbl">Total Costs</div><div class="kpi-val" style="color:var(--red)">'+rupees(totalCost)+'</div></div>'
    +'<div class="kpi"><div class="kpi-lbl">Net '+(net>=0?'Profit':'Loss')+'</div><div class="kpi-val" style="color:'+(net>=0?'var(--green)':'var(--red)')+'">'+rupees(Math.abs(net))+'</div></div>'
    +'</div>';
  html+='<div class="rpt-section"><div class="rpt-header">Profit & Loss Statement</div><table><thead><tr><th>Line Item</th><th>Amount</th></tr></thead><tbody>';
  html+='<tr><td style="color:var(--green);font-weight:600">Revenue</td><td></td></tr>';
  html+='<tr><td style="padding-left:30px">Flock Sales</td><td class="pnl-income">'+rupees(flockRev)+'</td></tr>';
  html+='<tr><td style="padding-left:30px">Sack / Feed Sales</td><td class="pnl-income">'+rupees(sackRev)+'</td></tr>';
  html+='<tr><td style="padding-left:30px">Other Revenue</td><td class="pnl-income">'+rupees(otherRev)+'</td></tr>';
  html+='<tr class="rpt-total-row"><td><strong>Total Revenue</strong></td><td class="pnl-income">'+rupees(totalRev)+'</td></tr>';
  html+='<tr><td style="color:var(--red);font-weight:600">Costs</td><td></td></tr>';
  html+='<tr><td style="padding-left:30px">Feed</td><td class="pnl-expense">'+rupees(feedCost)+'</td></tr>';
  html+='<tr><td style="padding-left:30px">Medicine</td><td class="pnl-expense">'+rupees(medCost)+'</td></tr>';
  html+='<tr><td style="padding-left:30px">Brada</td><td class="pnl-expense">'+rupees(bradaCost)+'</td></tr>';
  html+='<tr><td style="padding-left:30px">Payroll (processed)</td><td class="pnl-expense">'+rupees(payrollCost)+'</td></tr>';
  html+='<tr><td style="padding-left:30px">Labour (expenses)</td><td class="pnl-expense">'+rupees(labourExp)+'</td></tr>';
  html+='<tr><td style="padding-left:30px">Other Expenses</td><td class="pnl-expense">'+rupees(otherExp)+'</td></tr>';
  html+='<tr class="rpt-total-row"><td><strong>Total Costs</strong></td><td class="pnl-expense">'+rupees(totalCost)+'</td></tr>';
  html+='<tr class="rpt-total-row"><td><strong>Net '+(net>=0?'Profit':'Loss')+'</strong></td><td class="'+(net>=0?'pnl-profit':'pnl-loss')+'">'+rupees(Math.abs(net))+'</td></tr>';
  html+='</tbody></table></div>';return html;
}
function buildPerfReport(){
  if(!flocks.length)return '<div class="empty" style="padding:40px"><div class="ei">🐔</div><div class="et">No flocks registered yet.</div></div>';
  var html='<div class="rpt-section"><div class="rpt-header">Flock Performance Report</div><table><thead><tr><th>Flock</th><th>Breed</th><th>Status</th><th>Mortality</th><th>Mort%</th><th>Wt Records</th><th>Feed Used</th><th>Sales Revenue</th></tr></thead><tbody>';
  flocks.forEach(function(f){
  var mort=mortalities.filter(function(m){return m.flockId===f.id;}).reduce(function(s,m){return s+m.count;},0);
    var mortRate=f.origQty?((mort/f.origQty)*100).toFixed(1)+'%':'—';
  var wtCount=weeklyRecords.filter(function(w){return w.flockId===f.id;}).length;
    var feedUsed=feedTxns.filter(function(t){return t.type==='Usage'&&t.flockId===f.id;}).reduce(function(s,t){return s+t.qty;},0);
    var salesRev=flockSales.filter(function(s){return s.flockId===f.id;}).reduce(function(s,x){return s+x.total;},0);
    html+='<tr><td><span class="fc-id" style="font-size:0.75rem">'+esc(f.id)+'</span></td><td>'+esc(f.breed)+'</td>'
      +'<td><span class="badge '+(f.status==='Active'?'b-active':'b-closed')+'">'+f.status+'</span></td>'
      +'<td style="color:var(--red)">'+mort+'</td><td>'+mortRate+'</td><td>'+wtCount+'</td><td>'+feedUsed+' sacks</td>'
      +'<td style="font-weight:600;color:var(--green)">'+rupees(salesRev)+'</td></tr>';
  });
  html+='</tbody></table></div>';return html;
}
function buildSalesSummary(){
  var catMap={};
  flockSales.forEach(function(s){catMap['Flock Sales']=(catMap['Flock Sales']||0)+s.total;});
  var sackRev=totalSackSaleRevenue();
  if(sackRev>0)catMap['Sack / Feed Sales']=sackRev;
  otherSales.forEach(function(s){catMap[s.category]=(catMap[s.category]||0)+s.amount;});
  var total=Object.values(catMap).reduce(function(s,v){return s+v;},0);
  var html='<div class="rpt-section"><div class="rpt-header">Sales Summary</div>';
  if(!total){return html+'<div class="empty" style="padding:40px"><div class="ei">💰</div><div class="et">No sales recorded yet.</div></div></div>';}
  html+='<table><thead><tr><th>Category</th><th>Amount</th><th>% of Total</th></tr></thead><tbody>';
  Object.keys(catMap).forEach(function(cat){
    html+='<tr><td>'+esc(cat)+'</td><td style="font-weight:600;color:var(--green)">'+rupees(catMap[cat])+'</td><td>'+(total?(catMap[cat]/total*100).toFixed(1)+'%':'—')+'</td></tr>';
  });
  html+='<tr class="rpt-total-row"><td><strong>Total Revenue</strong></td><td class="pnl-income">'+rupees(total)+'</td><td>100%</td></tr>';
  html+='</tbody></table></div>';return html;
}
function buildExpSummary(){
  var catMap={};expenses.forEach(function(e){catMap[e.cat]=(catMap[e.cat]||0)+e.amount;});
  var total=Object.values(catMap).reduce(function(s,v){return s+v;},0);
  var html='<div class="rpt-section"><div class="rpt-header">Expense Summary by Category</div>';
  if(!total){return html+'<div class="empty" style="padding:40px"><div class="ei">📑</div><div class="et">No expenses recorded yet.</div></div></div>';}
  html+='<table><thead><tr><th>Category</th><th>Amount</th><th>% of Total</th></tr></thead><tbody>';
  Object.keys(catMap).sort(function(a,b){return catMap[b]-catMap[a];}).forEach(function(cat){
    html+='<tr><td>'+esc(cat)+'</td><td style="font-weight:600;color:var(--red)">'+rupees(catMap[cat])+'</td><td>'+(total?(catMap[cat]/total*100).toFixed(1)+'%':'—')+'</td></tr>';
  });
  html+='<tr class="rpt-total-row"><td><strong>Total Expenses</strong></td><td class="pnl-expense">'+rupees(total)+'</td><td>100%</td></tr>';
  html+='</tbody></table></div>';return html;
}
function buildResourceReport(){
  var html='<div class="rpt-section"><div class="rpt-header">Resource Consumption Report</div>';
  html+='<table><thead><tr><th>Resource</th><th>Total Purchased</th><th>Total Used</th><th>Current Stock</th><th>Total Cost</th></tr></thead><tbody>';
  // Feed
  var feedTypes=Object.keys(feedStock);
  feedTypes.forEach(function(t){
    var totalPurch=feedTxns.filter(function(x){return x.type==='Purchase'&&x.feedType===t;}).reduce(function(s,x){return s+x.qty;},0);
    var totalUsed=feedTxns.filter(function(x){return x.type==='Usage'&&x.feedType===t;}).reduce(function(s,x){return s+x.qty;},0);
    var totalCost=feedTxns.filter(function(x){return x.type==='Purchase'&&x.feedType===t;}).reduce(function(s,x){return s+(x.cost||0);},0);
    html+='<tr><td><strong>Feed: '+esc(t)+'</strong></td><td>'+totalPurch+' sacks</td><td>'+totalUsed+' sacks</td><td>'+feedStock[t].qty+' sacks</td><td>'+rupees(totalCost)+'</td></tr>';
  });
  // Medicine
  Object.keys(medStock).forEach(function(m){
    var totalPurch=medTxns.filter(function(x){return x.type==='Purchase'&&x.medicine===m;}).reduce(function(s,x){return s+x.qty;},0);
    var totalUsed=medTxns.filter(function(x){return x.type==='Usage'&&x.medicine===m;}).reduce(function(s,x){return s+x.qty;},0);
    var totalCost=medTxns.filter(function(x){return x.type==='Purchase'&&x.medicine===m;}).reduce(function(s,x){return s+(x.cost||0);},0);
    html+='<tr><td><strong>Medicine: '+esc(m)+'</strong></td><td>'+totalPurch+' units</td><td>'+totalUsed+' units</td><td>'+medStock[m].qty+' units</td><td>'+rupees(totalCost)+'</td></tr>';
  });
  // Brada
  if(bradaTxns.length){
    var bInv=computeBradaInventory();
    bradaStock=bInv.stock;
    html+='<tr><td><strong>Brada</strong></td><td>'+bInv.totalIn+' bags</td><td>'+bInv.totalOut+' bags</td><td>'+bInv.stock+' bags</td><td>'+rupees(bInv.totalCost)+'</td></tr>';
  }
  if(!feedTypes.length&&!Object.keys(medStock).length&&!bradaTxns.length)
    return html+'<tr><td colspan="5"><div class="empty" style="padding:20px"><div class="et">No resource records yet.</div></div></td></tr></tbody></table></div>';
  html+='</tbody></table></div>';return html;
}
function buildMedSupplierReport(){
  var supMap={};
  medTxns.filter(function(t){return t.type==='Purchase';}).forEach(function(t){
    var sn=t.supName||'Unknown';if(!supMap[sn])supMap[sn]=[];supMap[sn].push(t);
  });
  if(!Object.keys(supMap).length)return '<div class="empty" style="padding:40px"><div class="ei">💊</div><div class="et">No medicine purchases recorded yet.</div></div>';
  var html='<div class="rpt-section"><div class="rpt-header">Medicine Stock Report by Supplier</div>';
  Object.keys(supMap).forEach(function(sn){
    html+='<div style="margin-bottom:16px"><strong style="font-size:0.9rem">'+esc(sn)+'</strong><table style="margin-top:8px"><thead><tr><th>Medicine</th><th>Total Purchased</th><th>Total Used</th><th>Current Stock</th><th>Total Spent</th></tr></thead><tbody>';
    var medMap={};supMap[sn].forEach(function(t){if(!medMap[t.medicine])medMap[t.medicine]={purch:0,cost:0};medMap[t.medicine].purch+=t.qty;medMap[t.medicine].cost+=(t.cost||0);});
    Object.keys(medMap).forEach(function(m){
      var used=medTxns.filter(function(t){return t.type==='Usage'&&t.medicine===m;}).reduce(function(s,t){return s+t.qty;},0);
      var stock=medStock[m]?medStock[m].qty:0;
      html+='<tr><td>'+esc(m)+'</td><td>'+medMap[m].purch+'</td><td>'+used+'</td><td>'+stock+'</td><td>'+rupees(medMap[m].cost)+'</td></tr>';
    });
    html+='</tbody></table></div>';
  });
  html+='</div>';return html;
}
function buildBradaReport(){
  if(!bradaTxns.length)return '<div class="empty" style="padding:40px"><div class="ei">🪨</div><div class="et">No brada records yet.</div></div>';
  var inv=computeBradaInventory();
  bradaStock=inv.stock;
  var html='<div class="kpi-grid">'
    +'<div class="kpi"><div class="kpi-lbl">Total Purchased</div><div class="kpi-val">'+inv.totalIn+' bags</div></div>'
    +'<div class="kpi"><div class="kpi-lbl">Total Used</div><div class="kpi-val">'+inv.totalOut+' bags</div></div>'
    +'<div class="kpi"><div class="kpi-lbl">Current Stock</div><div class="kpi-val cv-g">'+inv.stock+' bags</div></div></div>';
  html+='<div class="rpt-section"><div class="rpt-header">Brada Transaction Log</div><table><thead><tr><th>Date</th><th>Type</th><th>Flock / Supplier</th><th>Qty (bags)</th><th>Cost</th><th>Balance</th></tr></thead><tbody>';
  inv.rows.forEach(function(row){
    var t=row.t;
    var who=t.type==='Purchase'?esc(t.flockId||'—')+' · '+esc(t.supName||'—'):esc(t.flockId||'—');
    html+='<tr><td>'+fmt(t.date)+'</td><td><strong style="color:'+(t.type==='Purchase'?'var(--green)':'var(--accent)')+'">'+esc(t.type)+'</strong></td>'
      +'<td>'+who+'</td><td>'+t.qty+'</td><td>'+(t.cost?rupees(t.cost):'—')+'</td><td style="font-weight:600">'+row.balAfter+'</td></tr>';
  });
  html+='<tr class="rpt-total-row"><td colspan="3"><strong>Summary</strong></td><td>In: '+inv.totalIn+' / Out: '+inv.totalOut+'</td><td>'+rupees(inv.totalCost)+'</td><td>'+inv.stock+'</td></tr>';
  html+='</tbody></table></div>';return html;
}
function buildFeedReport(){
  if(!feedTxns.length)return '<div class="empty" style="padding:40px"><div class="ei">🌾</div><div class="et">No feed transactions yet.</div></div>';
  var html='<div class="rpt-section"><div class="rpt-header">Feed Transaction Report</div><table><thead><tr><th>Date</th><th>Type</th><th>Feed Type</th><th>Flock/Supplier/Buyer</th><th>Sacks</th><th>Amount</th></tr></thead><tbody>';
  feedTxns.slice().sort(function(a,b){return b.date.localeCompare(a.date);}).forEach(function(t){
    html+='<tr><td>'+fmt(t.date)+'</td><td><strong style="color:'+(t.type==='Purchase'?'var(--green)':t.type==='Usage'?'var(--accent)':'var(--blue)')+'">'+t.type+'</strong></td>'
      +'<td>'+esc(t.feedType)+'</td><td>'+esc(t.flockId||t.supName||t.buyer||'—')+'</td><td>'+t.qty+'</td><td>'+(t.cost?rupees(t.cost):'—')+'</td></tr>';
  });
  var totalPurchCost=feedTxns.filter(function(t){return t.type==='Purchase';}).reduce(function(s,t){return s+(t.cost||0);},0);
  var totalSaleRev=feedTxns.filter(function(t){return t.type==='Sale';}).reduce(function(s,t){return s+(t.cost||0);},0);
  html+='<tr class="rpt-total-row"><td colspan="4"><strong>Totals</strong></td><td>—</td><td>Purchase cost: '+rupees(totalPurchCost)+' | Sale revenue: '+rupees(totalSaleRev)+'</td></tr>';
  html+='</tbody></table></div>';return html;
}

// ════════════════════════════════════════════════════
//  INIT
// ════════════════════════════════════════════════════
bindSupplierWorkerDelegation();
loadInitialData().then(function(){
  renderDash();
  renderSuppliers();
  renderPayroll();
  // Pre-set report tab
  document.querySelector('[data-rtab="mortality"]').classList.add('on');
});
