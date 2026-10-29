<?php
declare(strict_types=1);
require_once __DIR__ . '/bootstrap.php';
if ($_SERVER['REQUEST_METHOD'] !== 'POST') sendJson(405, ['ok'=>false,'error'=>'method_not_allowed']);
try { $uid=authenticateFirebaseUid(); $s=getDatabase()->prepare('UPDATE emergency_qr_tokens SET revoked_at=NOW() WHERE owner_firebase_uid=:uid AND revoked_at IS NULL'); $s->execute(['uid'=>$uid]); sendJson(200,['ok'=>true,'revoked'=>$s->rowCount()]); }
catch (Throwable $e) { error_log('FloodCue revoke_emergency_qr error: '.$e->getMessage()); sendJson(500,['ok'=>false,'error'=>'server_error']); }
