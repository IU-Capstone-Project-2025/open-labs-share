from fastapi import APIRouter, Form, Depends, HTTPException, Request, Response


router = APIRouter(tags=["Model"])

# @router.post("/predict", response_model=schemas.PredictionResponse)
# async def predict_credit_approve(
#     request: schemas.PredictionRequest,
#     predict_service: services.PredictCreditService = Depends(dependencies.get_predict_credit_service),
# ) -> schemas.PredictionResponse:
#     try:
#         pred, proba = predict_service.predict(request)
#         return schemas.PredictionResponse(pred=pred, proba=proba)
#     except Exception as e:
#         raise HTTPException(status_code=500, detail=str(e))


# @router.post("/ask", response_model=schemas.PredictionResponse)
# async def ask(

# ):
#     try:
#         pass
#     except Exception as e:
#         pass